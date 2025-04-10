package com.exemplo.catraca_connection;

import org.junit.jupiter.api.Test;

import com.zktecodevice.turnstile_connection.TurnstileHttpClient;

public class MonitoramentoCatracasTest {

	@Test
	public void testValidarCracha() {
		String ipCatraca = "99.99.9.999";
		int porta = 2;
		String leitura = "1561567809";

		String resultado = TurnstileHttpClient.validateCard(ipCatraca, (long) porta, leitura);

		if (resultado != null && resultado.equalsIgnoreCase("true")) {
			System.out.println("Pessoa válida!");
		} else {
			System.out.println("Pessoa inválida!");
		}

		System.out.println("Resultado da validação: " + resultado);

	}
}
