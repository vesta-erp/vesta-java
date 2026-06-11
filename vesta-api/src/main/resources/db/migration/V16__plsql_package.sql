-- =============================================================
-- VESTA - Package PKG_GESTAO_ABRIGO
-- Centraliza operações críticas de gestão de abrigos.
-- Depende das functions criadas em V13.
-- =============================================================

-- SPECIFICATION
CREATE OR REPLACE PACKAGE PKG_GESTAO_ABRIGO AS

    -- Registra saída de pessoa do abrigo e decrementa ocupação
    PROCEDURE SP_REGISTRAR_SAIDA(
        p_id_pessoa  IN NUMBER,
        p_id_usuario IN NUMBER
    );

    -- Cancela solicitações ABERTA/EM_ANALISE sem atualização há mais de N dias
    PROCEDURE SP_FECHAR_SOLICITACOES_ANTIGAS(
        p_dias IN NUMBER DEFAULT 30
    );

    -- Retorna nível de criticidade (wrapper sobre FN_NIVEL_CRITICIDADE)
    FUNCTION FN_CRITICIDADE(p_id_abrigo IN NUMBER) RETURN VARCHAR2;

    -- Retorna ocupação formatada: "atual/capacidade (taxa%)"
    FUNCTION FN_OCUPACAO_FORMATADA(p_id_abrigo IN NUMBER) RETURN VARCHAR2;

END PKG_GESTAO_ABRIGO;
/

-- BODY
CREATE OR REPLACE PACKAGE BODY PKG_GESTAO_ABRIGO AS

    PROCEDURE SP_REGISTRAR_SAIDA(
        p_id_pessoa  IN NUMBER,
        p_id_usuario IN NUMBER
    ) IS
        v_id_abrigo NUMBER;
        v_presente  CHAR(1);
    BEGIN
        SELECT id_abrigo, st_presente
        INTO   v_id_abrigo, v_presente
        FROM   TB_PESSOA_ABRIGADA
        WHERE  id_pessoa = p_id_pessoa;

        IF v_presente = 'N' THEN
            RAISE_APPLICATION_ERROR(-20020, 'Pessoa já registrou saída anteriormente.');
        END IF;

        UPDATE TB_PESSOA_ABRIGADA
        SET    st_presente = 'N', dt_saida = SYSDATE
        WHERE  id_pessoa = p_id_pessoa;

        UPDATE TB_ABRIGO
        SET    qt_ocupacao_atual = GREATEST(qt_ocupacao_atual - 1, 0),
               st_status = CASE
                   WHEN st_status = 'LOTADO' THEN 'ATIVO'
                   ELSE st_status
               END
        WHERE  id_abrigo = v_id_abrigo;

        COMMIT;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20021, 'Pessoa não encontrada: ' || p_id_pessoa);
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE;
    END SP_REGISTRAR_SAIDA;

    PROCEDURE SP_FECHAR_SOLICITACOES_ANTIGAS(
        p_dias IN NUMBER DEFAULT 30
    ) IS
        v_count NUMBER := 0;
    BEGIN
        UPDATE TB_SOLICITACAO_RECURSO
        SET    st_status      = 'CANCELADA',
               dt_atualizacao = SYSDATE
        WHERE  st_status IN ('ABERTA', 'EM_ANALISE')
        AND    dt_atualizacao < SYSDATE - p_dias;

        v_count := SQL%ROWCOUNT;
        COMMIT;
        DBMS_OUTPUT.PUT_LINE(v_count || ' solicitações inativas canceladas após ' ||
                             p_dias || ' dias sem atualização.');
    END SP_FECHAR_SOLICITACOES_ANTIGAS;

    FUNCTION FN_CRITICIDADE(p_id_abrigo IN NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN FN_NIVEL_CRITICIDADE(p_id_abrigo);
    END FN_CRITICIDADE;

    FUNCTION FN_OCUPACAO_FORMATADA(p_id_abrigo IN NUMBER) RETURN VARCHAR2 IS
        v_taxa    NUMBER;
        v_ocup    NUMBER;
        v_cap     NUMBER;
    BEGIN
        SELECT qt_ocupacao_atual, qt_capacidade_maxima
        INTO   v_ocup, v_cap
        FROM   TB_ABRIGO
        WHERE  id_abrigo = p_id_abrigo;

        v_taxa := FN_TAXA_OCUPACAO(p_id_abrigo);
        RETURN v_ocup || '/' || v_cap || ' (' || v_taxa || '%)';
    EXCEPTION
        WHEN NO_DATA_FOUND THEN RETURN 'N/A';
    END FN_OCUPACAO_FORMATADA;

END PKG_GESTAO_ABRIGO;
/
