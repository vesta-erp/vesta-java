-- =============================================================
-- VESTA - Triggers PL/SQL
-- TRG_ATUALIZA_OCUPACAO, TRG_ALERTA_ESTOQUE, TRG_VALIDA_CAPACIDADE
-- =============================================================

-- TRIGGER 1: Recalcula qt_ocupacao_atual ao inserir/deletar pessoa
-- Para uso standalone dos scripts Oracle. Quando a API Java está em uso,
-- FamiliaService e TransferenciaService emitem o UPDATE final em TB_ABRIGO.
CREATE OR REPLACE TRIGGER TRG_ATUALIZA_OCUPACAO
AFTER INSERT OR DELETE ON TB_PESSOA_ABRIGADA
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        UPDATE TB_ABRIGO
        SET    qt_ocupacao_atual = qt_ocupacao_atual + 1,
               st_status = CASE
                   WHEN qt_ocupacao_atual + 1 >= qt_capacidade_maxima THEN 'LOTADO'
                   ELSE st_status
               END
        WHERE  id_abrigo = :NEW.id_abrigo;

    ELSIF DELETING THEN
        UPDATE TB_ABRIGO
        SET    qt_ocupacao_atual = GREATEST(qt_ocupacao_atual - 1, 0),
               st_status = CASE
                   WHEN st_status = 'LOTADO' AND qt_ocupacao_atual - 1 < qt_capacidade_maxima
                   THEN 'ATIVO'
                   ELSE st_status
               END
        WHERE  id_abrigo = :OLD.id_abrigo;
    END IF;
END TRG_ATUALIZA_OCUPACAO;
/

-- TRIGGER 2: Gera alerta quando estoque cai abaixo do mínimo
-- Dedup: não cria alerta duplicado se já existe um ATIVO para o mesmo recurso/abrigo
CREATE OR REPLACE TRIGGER TRG_ALERTA_ESTOQUE
AFTER UPDATE OF qt_atual ON TB_ESTOQUE_ABRIGO
FOR EACH ROW
WHEN (NEW.qt_atual < NEW.qt_minima AND OLD.qt_atual >= OLD.qt_minima)
DECLARE
    v_nm_recurso TB_RECURSO.nm_recurso%TYPE;
    v_count      NUMBER;
BEGIN
    SELECT nm_recurso INTO v_nm_recurso
    FROM   TB_RECURSO WHERE id_recurso = :NEW.id_recurso;

    SELECT COUNT(*) INTO v_count
    FROM   TB_ALERTA
    WHERE  id_abrigo  = :NEW.id_abrigo
    AND    tp_alerta  = 'ESTOQUE_CRITICO'
    AND    id_recurso = :NEW.id_recurso
    AND    st_status  = 'ATIVO';

    IF v_count = 0 THEN
        INSERT INTO TB_ALERTA (id_abrigo, id_recurso, tp_alerta, ds_mensagem)
        VALUES (:NEW.id_abrigo, :NEW.id_recurso, 'ESTOQUE_CRITICO',
                'Recurso "' || v_nm_recurso || '" abaixo do mínimo: ' ||
                :NEW.qt_atual || ' (mínimo: ' || :NEW.qt_minima || ')');
    END IF;
END TRG_ALERTA_ESTOQUE;
/

-- TRIGGER 3: Valida capacidade antes de inserir pessoa
CREATE OR REPLACE TRIGGER TRG_VALIDA_CAPACIDADE
BEFORE INSERT ON TB_PESSOA_ABRIGADA
FOR EACH ROW
DECLARE
    v_capacidade NUMBER;
    v_ocupacao   NUMBER;
    v_status     VARCHAR2(20);
BEGIN
    SELECT qt_capacidade_maxima, qt_ocupacao_atual, st_status
    INTO   v_capacidade, v_ocupacao, v_status
    FROM   TB_ABRIGO
    WHERE  id_abrigo = :NEW.id_abrigo;

    IF v_status = 'INTERDITADO' THEN
        RAISE_APPLICATION_ERROR(-20010,
            'Abrigo interditado. Não é possível registrar pessoas.');
    END IF;

    IF v_ocupacao >= v_capacidade OR v_status = 'LOTADO' THEN
        RAISE_APPLICATION_ERROR(-20011,
            'Abrigo com capacidade máxima atingida (' || v_capacidade || ' pessoas).');
    END IF;
END TRG_VALIDA_CAPACIDADE;
/
