package com.zktecodevice.device_monitoring;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zktecodevice.catraca_connection.CatracaHttpClient;

public class MonitoramentoCatracas {
    private static final Logger logger = LoggerFactory.getLogger(MonitoramentoCatracas.class);

    private int conexao;
    private String ip;
    private ScheduledExecutorService scheduler;

    public MonitoramentoCatracas(String ip) throws Exception {
        this.ip = ip;
        conectar();
    }

    private void conectar() throws Exception {
        while (true) {
            String params = "protocol=TCP,ipaddress=" + ip + ",port=4370,timeout=10000,passwd=";
            conexao = CatracaSDK.Plcommpro.INSTANCE.Connect(params);
            if (conexao != 0) {
                logger.info("Conectado com sucesso à controladora em: {} | ID da conexão: {}", ip, conexao);
                return;
            } else {
                logger.warn("Falha ao conectar à controladora em: {}.", ip);
            }
            Thread.sleep(2000);
        }
    }

    public void desconectar() {
        if (conexao != 0) {
            CatracaSDK.Plcommpro.INSTANCE.Disconnect(conexao);
            logger.info("Conexão com o IP {} encerrada.", ip);
        }
    }

    private void reconectar() {
        try {
            desconectar();
            conectar();
        } catch (Exception e) {
            logger.error("Falha ao reconectar ao IP: {}", ip, e);
        }
    }

    public void iniciarMonitoramento() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorarEventos();
            } catch (Exception e) {
                logger.error("Erro durante o monitoramento: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void monitorarEventos() {
        byte[] buffer = new byte[4096];

        int resultados = CatracaSDK.Plcommpro.INSTANCE.GetRTLog(conexao, buffer, buffer.length);
        if (resultados > 0) {
            String data = new String(buffer).trim();
            String[] eventos = data.split("\r\n");
            for (String registro : eventos) {
                processarEventos(registro);
            }
        } else if (resultados == -2) {
            logger.warn("Perda de conexão com o IP: {}. Tentando reconectar...", ip);
            reconectar();
        } else {
            logger.error("Erro desconhecido ao monitorar eventos. Código de retorno: {}", resultados);
        }
    }

    private void processarEventos(String registro) {
        String[] valores = registro.split(",");
        if (valores.length < 7) {
            logger.warn("Registro inválido ou incompleto: {}", registro);
            return;
        }
        
        try {
            int bit4 = Integer.parseInt(valores[4].trim());
            if (bit4 == 255) {
                logger.debug("Registro de status de porta/alarme ignorado: {}", registro);
                return;
            }

            String horarioEvento = valores[0].trim();
            String numeroCartao = valores[2].trim();
            int numeracaoPorta = Integer.parseInt(valores[3].trim());
            int codigoEvento = Integer.parseInt(valores[4].trim());
            int direcaoPassagem = Integer.parseInt(valores[5].trim());

            logger.info("Evento detectado: Hora: {} | Cartão: {} | Porta: {} | Tipo de Evento: {} | Direção: {}",
                    horarioEvento, numeroCartao, numeracaoPorta, codigoEvento, direcoesPassagem(direcaoPassagem));

            String resultadoValidacao = CatracaHttpClient.validarCracha(ip, (long) numeracaoPorta, numeroCartao);

            if ("true".equalsIgnoreCase(resultadoValidacao)) {
                liberarPassagemComMonitoramento(numeracaoPorta);
            } else {
                logger.warn("Crachá inválido ou não autorizado: {}", numeroCartao);
            }

        } catch (NumberFormatException e) {
            logger.error("Erro ao processar registro: {}", registro, e);
        }
    }

    private static String direcoesPassagem(int status) {
        switch (status) {
            case 0:
                return "Entrada";
            case 1:
                return "Saída";
            case 2:
                return "Nenhum";
            default:
                return "Desconhecido";
        }
    }
    
    public void liberarPassagemComMonitoramento(int porta) {
        int tempoAbertura = 5;
        
        boolean sucesso = controlarDispositivo(porta, tempoAbertura);
        if (sucesso) {
            logger.info("Catraca liberada para a porta: {}", porta);
            verificarPassagem(porta, tempoAbertura);
        } else {
            logger.error("Erro inesperado. Código: {}");
        }
    }
    
    private boolean controlarDispositivo(int porta, int tempoAbertura) {
        int resultado = CatracaSDK.Plcommpro.INSTANCE.ControlDevice(conexao, 1, porta, 1, tempoAbertura, 0, "");
        if (resultado >= 0) {
            System.out.println("sucesso");
            return true;
        } else {
            logger.error("Erro ao liberar a catraca. Código: {}", resultado);
            reconectar();
            return false;
        }
    }
    
    private void verificarPassagem(int porta, int tempoAbertura) {
        long startTime = System.currentTimeMillis();
        boolean passagemConfirmada = false;
        
        while (System.currentTimeMillis() - startTime < 10 * 1000) {
            try {
                byte[] buffer = new byte[4096];
                int resultados = CatracaSDK.Plcommpro.INSTANCE.GetRTLog(conexao, buffer, buffer.length);
                
                if (resultados > 0) {
                    String data = new String(buffer).trim();
                    String[] eventos = data.split("\r\n");
                    
                    for (String registro : eventos) {
                        String[] valores = registro.split(",");
                        
                        if (valores.length < 7) {
                        	logger.warn("Registro inválido ou incompleto: {}", registro);
                            continue;
                        }
                        
                        String horarioEvento = valores[0].trim();
                        String numeroCartao = valores[2].trim();
                        int direcaoPassagem = Integer.parseInt(valores[5].trim());
                        int numeracaoPorta = Integer.parseInt(valores[3].trim());
                        int codigoEvento = Integer.parseInt(valores[4].trim());
                        
                        logger.info("Evento detectado: Hora: {} | Cartão: {} | Porta: {} | Tipo de Evento: {} | Direção: {}",
                                horarioEvento, numeroCartao, numeracaoPorta, codigoEvento, direcoesPassagem(direcaoPassagem));
                        
                        if (numeracaoPorta == porta && (codigoEvento == 200 || codigoEvento == 201)) {
                            CatracaHttpClient.registrarAcesso(ip, (long) porta);
                            logger.info("Passagem confirmada pela catraca para a porta {}", porta);
                            passagemConfirmada = true;
                            break;
                        }
                    }
                }
                
                if (passagemConfirmada) {
                    break;
                }
            } catch (NumberFormatException e) {
                logger.error("Erro ao verificar passagem: {}", e.getMessage());
                break;
            }
        }
        
        if (!passagemConfirmada) {
            CatracaHttpClient.registrarDesistencia(ip, (long) porta);
            logger.warn("Passagem não detectada para a porta {}", porta);
        }
    }
}
