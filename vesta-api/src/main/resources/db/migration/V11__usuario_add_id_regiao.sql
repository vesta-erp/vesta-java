ALTER TABLE TB_USUARIO ADD id_regiao NUMBER(19);
ALTER TABLE TB_USUARIO ADD CONSTRAINT fk_usuario_regiao
    FOREIGN KEY (id_regiao) REFERENCES TB_REGIAO (id_regiao);

-- Backfill: vincular GESTORs do seed V7 às suas regiões correspondentes
UPDATE TB_USUARIO SET id_regiao = (SELECT id_regiao FROM TB_REGIAO WHERE nm_regiao = 'Grande São Paulo')
WHERE ds_email = 'ana.sp@vesta.gov.br';

UPDATE TB_USUARIO SET id_regiao = (SELECT id_regiao FROM TB_REGIAO WHERE nm_regiao = 'Vale do Paraíba')
WHERE ds_email = 'bruno.sjc@vesta.gov.br';

UPDATE TB_USUARIO SET id_regiao = (SELECT id_regiao FROM TB_REGIAO WHERE nm_regiao = 'Baixada Fluminense')
WHERE ds_email = 'diana.rj@vesta.gov.br';

UPDATE TB_USUARIO SET id_regiao = (SELECT id_regiao FROM TB_REGIAO WHERE nm_regiao = 'Região Metropolitana BH')
WHERE ds_email = 'eduardo.mg@vesta.gov.br';

UPDATE TB_USUARIO SET id_regiao = (SELECT id_regiao FROM TB_REGIAO WHERE nm_regiao = 'Litoral Sul RS')
WHERE ds_email = 'fernanda.rs@vesta.gov.br';
