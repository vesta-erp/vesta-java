-- =============================================================
-- VESTA - Corrige hashes de senha inválidos para usuários de teste
-- operador123 → perfil OPERADOR
-- =============================================================

UPDATE TB_USUARIO SET ds_senha_hash = '$2a$10$YwbK7L3yZXUDSt7JyUXuJu7cgrSzQ96LkS4Pk2eeSEzb4i8TNJJty'
WHERE ds_email IN (
    'operador.teste@vesta.gov.br',
    'operador.centro@vesta.gov.br'
);
