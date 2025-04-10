package com.exemplo.catraca_connection;

import org.junit.jupiter.api.Test;

import com.zktecodevice.catraca_connection.CatracaHttpClient;

public class MonitoramentoCatracasTest {

	@Test
	public void testValidarCracha() {
		String ipCatraca = "10.10.7.218";
		int porta = 2;
		String leitura = "1561567809";

		String resultado = CatracaHttpClient.validarCracha(ipCatraca, (long) porta, leitura);

		if (resultado != null && resultado.equalsIgnoreCase("true")) {
			System.out.println("Pessoa válida!");
		} else {
			System.out.println("Pessoa inválida!");
		}

		System.out.println("Resultado da validação: " + resultado);

	}
}
