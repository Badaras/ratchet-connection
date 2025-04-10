package com.exemplo.catraca_connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.zktecodevice.turnstile_connection.TurnstileHttpClient;

public class RegistrarDesistenciaTest {

    @Test
    public void testRegistrarDesistenciaComDadosValidos() {
        String ipCatraca = "99.99.9.999";
        int porta = 1;

        try {
            TurnstileHttpClient.registerAccess(ipCatraca, (long) porta);
            assertTrue(true);
        } catch (Exception e) {
            fail("Erro ao registrar desistência: " + e.getMessage());
        }
    }

    @Test
    public void testRegistrarDesistenciaComDadosInvalidos() {
        String ipCatraca = null;
        int porta = 2;

        try {
            TurnstileHttpClient.registerAccess(ipCatraca, (long) porta);
            System.out.println("Deveria ter lançado uma exceção para dados inválidos.");
        } catch (IllegalArgumentException e) {
            assertEquals("Parâmetros inválidos para registrar desistência.", e.getMessage());
        }
    }
}
