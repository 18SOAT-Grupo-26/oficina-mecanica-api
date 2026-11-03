package br.com.fiap.oficinamecanicaapi.integracao;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Pula os testes de integração quando não há Docker na máquina, em vez de deixar o contexto do Spring quebrar ao
 * subir o PostgreSQL. É uma extensão, e não um {@code @EnabledIf}, porque o {@code @ExtendWith} é herdado pelas
 * subclasses de {@link IntegracaoBase}; o {@code @EnabledIf} não é, e por isso nunca chegava aos {@code *IT}.
 */
public class DockerDisponivel implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return dockerDisponivel()
                ? ConditionEvaluationResult.enabled("Docker disponível")
                : ConditionEvaluationResult.disabled("Docker indisponível: testes de integração pulados");
    }

    private static boolean dockerDisponivel() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception _) {
            return false;
        }
    }
}
