package com.exemplo.catraca_connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.zktecodevice.catraca_connection.CatracaHttpClient;

public class RegistrarAcessoTest {

    @Test
    public void testRegistrarAcessoComDadosValidos() {
        String ipCatraca = "99.99.9.999";
        int porta = 1;

        try {
            CatracaHttpClient.registrarAcesso(ipCatraca, (long) porta);
            assertTrue(true);
        } catch (Exception e) {
            fail("Erro ao registrar acesso: " + e.getMessage());
        }
    }

    @Test
    public void testRegistrarAcessoComDadosInvalidos() {
        String ipCatraca = "99.99.9.999";
        int porta = -100;

        try {
            CatracaHttpClient.registrarAcesso(ipCatraca, (long) porta);
            System.out.println("Deveria ter lançado uma exceção para dados inválidos.");
        } catch (IllegalArgumentException e) {
            assertEquals("Parâmetros inválidos para registrar acesso.", e.getMessage());
        }
    }
}
