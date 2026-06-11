-- =============================================================
-- VESTA - Procedures PL/SQL
-- SP_REGISTRAR_ACOLHIMENTO, SP_ATENDER_SOLICITACAO, SP_DASHBOARD_REGIONAL
-- Dependem das functions criadas em V13.
-- =============================================================

-- PROCEDURE 1: Registrar acolhimento de família
-- Insere família, atualiza ocupação, gera alerta de lotação se >= 90%
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_ACOLHIMENTO(
    p_nm_responsavel  IN VARCHAR2,
    p_nr_cpf          IN VARCHAR2,
    p_nr_telefone     IN VARCHAR2,
    p_id_abrigo       IN NUMBER,
    p_qt_pessoas      IN NUMBER,
    p_id_usuario      IN NUMBER
) IS
    v_vagas       NUMBER;
    v_taxa        NUMBER;
    v_id_familia  NUMBER;
BEGIN
    v_vagas := FN_VAGAS_DISPONIVEIS(p_id_abrigo);
    IF v_vagas < p_qt_pessoas THEN
        RAISE_APPLICATION_ERROR(-20001,
            'Capacidade insuficiente. Vagas disponíveis: ' || v_vagas);
    END IF;

    INSERT INTO TB_FAMILIA (nm_responsavel, nr_cpf_responsavel, nr_telefone, id_abrigo)
    VALUES (p_nm_responsavel, p_nr_cpf, p_nr_telefone, p_id_abrigo)
    RETURNING id_familia INTO v_id_familia;

    UPDATE TB_ABRIGO
    SET    qt_ocupacao_atual = qt_ocupacao_atual + p_qt_pessoas
    WHERE  id_abrigo = p_id_abrigo;

    UPDATE TB_ABRIGO
    SET    st_status = 'LOTADO'
    WHERE  id_abrigo = p_id_abrigo
    AND    qt_ocupacao_atual >= qt_capacidade_maxima;

    v_taxa := FN_TAXA_OCUPACAO(p_id_abrigo);
    IF v_taxa >= 90 THEN
        INSERT INTO TB_ALERTA (id_abrigo, tp_alerta, ds_mensagem)
        VALUES (p_id_abrigo, 'LOTACAO',
                'Abrigo ' || p_id_abrigo || ' com ' || v_taxa || '% de ocupação.');
    END IF;

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END SP_REGISTRAR_ACOLHIMENTO;
/

-- PROCEDURE 2: Atender solicitação de recurso
-- Atualiza status → CONCLUIDA, cria movimentação de entrada, atualiza estoque,
-- e resolve alertas de estoque crítico do recurso atendido
CREATE OR REPLACE PROCEDURE SP_ATENDER_SOLICITACAO(
    p_id_solicitacao IN NUMBER,
    p_id_usuario     IN NUMBER
) IS
    v_id_abrigo   NUMBER;
    v_id_recurso  NUMBER;
    v_qt_solic    NUMBER;
    v_st_atual    VARCHAR2(20);
BEGIN
    SELECT id_abrigo, id_recurso, qt_solicitada, st_status
    INTO   v_id_abrigo, v_id_recurso, v_qt_solic, v_st_atual
    FROM   TB_SOLICITACAO_RECURSO
    WHERE  id_solicitacao = p_id_solicitacao;

    IF v_st_atual = 'CONCLUIDA' THEN
        RAISE_APPLICATION_ERROR(-20002, 'Solicitação já concluída.');
    END IF;
    IF v_st_atual = 'CANCELADA' THEN
        RAISE_APPLICATION_ERROR(-20003, 'Solicitação cancelada. Não pode ser atendida.');
    END IF;

    UPDATE TB_SOLICITACAO_RECURSO
    SET    st_status = 'CONCLUIDA', dt_atualizacao = SYSDATE
    WHERE  id_solicitacao = p_id_solicitacao;

    INSERT INTO TB_MOVIMENTACAO_RECURSO
        (id_solicitacao, id_abrigo, id_recurso, id_usuario, tp_movimentacao, qt_movimentada, ds_observacao)
    VALUES
        (p_id_solicitacao, v_id_abrigo, v_id_recurso, p_id_usuario,
         'ENTRADA', v_qt_solic, 'Atendimento de solicitação #' || p_id_solicitacao);

    MERGE INTO TB_ESTOQUE_ABRIGO e
    USING DUAL
    ON (e.id_abrigo = v_id_abrigo AND e.id_recurso = v_id_recurso)
    WHEN MATCHED THEN
        UPDATE SET qt_atual = qt_atual + v_qt_solic, dt_atualizacao = SYSDATE
    WHEN NOT MATCHED THEN
        INSERT (id_abrigo, id_recurso, qt_atual, qt_minima)
        VALUES (v_id_abrigo, v_id_recurso, v_qt_solic, 0);

    UPDATE TB_ALERTA
    SET    st_status = 'RESOLVIDO', dt_resolucao = SYSDATE
    WHERE  id_abrigo  = v_id_abrigo
    AND    tp_alerta  = 'ESTOQUE_CRITICO'
    AND    id_recurso = v_id_recurso
    AND    st_status  = 'ATIVO';

    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20004, 'Solicitação não encontrada: ' || p_id_solicitacao);
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END SP_ATENDER_SOLICITACAO;
/

-- PROCEDURE 3: Consolidar dashboard regional
-- Imprime relatório de abrigos ativos de uma região, ordenados por criticidade
CREATE OR REPLACE PROCEDURE SP_DASHBOARD_REGIONAL(
    p_id_regiao IN NUMBER
) IS
    CURSOR c_abrigos IS
        SELECT a.id_abrigo,
               a.nm_abrigo,
               a.qt_capacidade_maxima,
               a.qt_ocupacao_atual,
               a.st_status
        FROM   TB_ABRIGO a
        WHERE  a.id_regiao = p_id_regiao
        AND    a.st_status <> 'INATIVO'
        ORDER BY FN_TAXA_OCUPACAO(a.id_abrigo) DESC;

    v_taxa        NUMBER;
    v_criticidade VARCHAR2(10);
    v_vagas       NUMBER;
    v_nm_regiao   TB_REGIAO.nm_regiao%TYPE;
BEGIN
    SELECT nm_regiao INTO v_nm_regiao
    FROM   TB_REGIAO WHERE id_regiao = p_id_regiao;

    DBMS_OUTPUT.PUT_LINE('DASHBOARD REGIONAL: ' || v_nm_regiao);
    DBMS_OUTPUT.PUT_LINE('Data: ' || TO_CHAR(SYSDATE, 'DD/MM/YYYY HH24:MI'));

    FOR r IN c_abrigos LOOP
        v_taxa        := FN_TAXA_OCUPACAO(r.id_abrigo);
        v_criticidade := FN_NIVEL_CRITICIDADE(r.id_abrigo);
        v_vagas       := FN_VAGAS_DISPONIVEIS(r.id_abrigo);

        DBMS_OUTPUT.PUT_LINE(
            r.nm_abrigo || ' [' || r.st_status || ']' ||
            ' | Ocup: ' || r.qt_ocupacao_atual || '/' || r.qt_capacidade_maxima ||
            ' (' || v_taxa || '%)' ||
            ' | Vagas: ' || v_vagas ||
            ' | Criticidade: ' || v_criticidade
        );
    END LOOP;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20005, 'Região não encontrada: ' || p_id_regiao);
END SP_DASHBOARD_REGIONAL;
/
