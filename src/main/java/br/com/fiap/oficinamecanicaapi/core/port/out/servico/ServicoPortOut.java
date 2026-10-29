package br.com.fiap.oficinamecanicaapi.core.port.out.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Optional;

public interface ServicoPortOut {

    Servico salvar(Servico servico);

    Optional<Servico> buscarPorId(Long id);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    void excluir(Servico servico);

    Pagina<Servico> listar(int pagina, int tamanho);
}
