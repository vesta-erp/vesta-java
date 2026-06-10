package br.com.fiap.vesta.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VestaErrorCodeTest {

    @Test
    void deveTerTodosOsNoveCodigosDeErro() {
        assertThat(VestaErrorCode.values()).hasSize(9);
        assertThat(VestaErrorCode.valueOf("RECURSO_NAO_ENCONTRADO")).isNotNull();
        assertThat(VestaErrorCode.valueOf("REGRA_DE_NEGOCIO_VIOLADA")).isNotNull();
        assertThat(VestaErrorCode.valueOf("CONFLITO_DE_DADOS")).isNotNull();
        assertThat(VestaErrorCode.valueOf("ACESSO_NEGADO")).isNotNull();
        assertThat(VestaErrorCode.valueOf("AUTENTICACAO_FALHOU")).isNotNull();
        assertThat(VestaErrorCode.valueOf("CAMPOS_INVALIDOS")).isNotNull();
        assertThat(VestaErrorCode.valueOf("LIMITE_CONEXOES_DB")).isNotNull();
        assertThat(VestaErrorCode.valueOf("ERRO_BANCO_DE_DADOS")).isNotNull();
        assertThat(VestaErrorCode.valueOf("ERRO_INTERNO")).isNotNull();
    }
}
