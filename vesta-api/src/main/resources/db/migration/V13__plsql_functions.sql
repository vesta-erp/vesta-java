-- =============================================================
-- VESTA - Functions PL/SQL
-- FN_TAXA_OCUPACAO, FN_NIVEL_CRITICIDADE, FN_VAGAS_DISPONIVEIS
-- =============================================================

-- FUNCTION 1: Taxa de ocupação do abrigo (0 a 100%)
CREATE OR REPLACE FUNCTION FN_TAXA_OCUPACAO(p_id_abrigo IN NUMBER)
RETURN NUMBER IS
    v_capacidade  TB_ABRIGO.qt_capacidade_maxima%TYPE;
    v_ocupacao    TB_ABRIGO.qt_ocupacao_atual%TYPE;
BEGIN
    SELECT qt_capacidade_maxima, qt_ocupacao_atual
    INTO   v_capacidade, v_ocupacao
    FROM   TB_ABRIGO
    WHERE  id_abrigo = p_id_abrigo;

    IF v_capacidade = 0 THEN
        RETURN 0;
    END IF;

    RETURN ROUND((v_ocupacao / v_capacidade) * 100, 2);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN -1;
END FN_TAXA_OCUPACAO;
/

-- FUNCTION 2: Nível de criticidade do abrigo
-- Score: ocupação (0-50 pts) + recursos abaixo do mínimo (0-30 pts) + ocorrências críticas (0-20 pts)
CREATE OR REPLACE FUNCTION FN_NIVEL_CRITICIDADE(p_id_abrigo IN NUMBER)
RETURN VARCHAR2 IS
    v_taxa_ocup      NUMBER;
    v_rec_criticos   NUMBER;
    v_ocorr_criticas NUMBER;
    v_score          NUMBER := 0;
BEGIN
    v_taxa_ocup := FN_TAXA_OCUPACAO(p_id_abrigo);

    SELECT COUNT(*) INTO v_rec_criticos
    FROM   TB_ESTOQUE_ABRIGO
    WHERE  id_abrigo = p_id_abrigo
    AND    qt_atual < qt_minima;

    SELECT COUNT(*) INTO v_ocorr_criticas
    FROM   TB_OCORRENCIA
    WHERE  id_abrigo = p_id_abrigo
    AND    st_status IN ('ABERTA', 'EM_ANDAMENTO')
    AND    tp_severidade IN ('ALTA', 'CRITICA');

    IF    v_taxa_ocup >= 90 THEN v_score := v_score + 50;
    ELSIF v_taxa_ocup >= 75 THEN v_score := v_score + 30;
    ELSIF v_taxa_ocup >= 50 THEN v_score := v_score + 15;
    END IF;

    v_score := v_score + LEAST(v_rec_criticos * 10, 30);
    v_score := v_score + LEAST(v_ocorr_criticas * 10, 20);

    IF    v_score >= 70 THEN RETURN 'CRITICO';
    ELSIF v_score >= 40 THEN RETURN 'ALTO';
    ELSIF v_score >= 20 THEN RETURN 'MEDIO';
    ELSE                     RETURN 'BAIXO';
    END IF;
END FN_NIVEL_CRITICIDADE;
/

-- FUNCTION 3: Vagas disponíveis no abrigo
CREATE OR REPLACE FUNCTION FN_VAGAS_DISPONIVEIS(p_id_abrigo IN NUMBER)
RETURN NUMBER IS
    v_capacidade NUMBER;
    v_ocupacao   NUMBER;
BEGIN
    SELECT qt_capacidade_maxima, qt_ocupacao_atual
    INTO   v_capacidade, v_ocupacao
    FROM   TB_ABRIGO
    WHERE  id_abrigo = p_id_abrigo;

    RETURN GREATEST(v_capacidade - v_ocupacao, 0);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN -1;
END FN_VAGAS_DISPONIVEIS;
/
