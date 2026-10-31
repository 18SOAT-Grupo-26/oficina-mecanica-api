package br.com.fiap.oficinamecanicaapi.core.port.out.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Optional;

public interface ServicoPortOut {

    Servico salvar(Servico servico);

    Optional<Servico> buscarAtivoPorId(Long id);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    Pagina<Servico> listarAtivos(int pagina, int tamanho);
}
