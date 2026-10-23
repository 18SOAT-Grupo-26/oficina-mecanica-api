package br.com.fiap.oficinamecanicaapi.core.port.out.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Optional;

public interface ServicoPortOut {

    ServicoEntity salvar(ServicoEntity servico);

    Optional<ServicoEntity> buscarPorId(Long id);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    void excluir(ServicoEntity servico);

    Pagina<ServicoEntity> listar(int pagina, int tamanho);
}
