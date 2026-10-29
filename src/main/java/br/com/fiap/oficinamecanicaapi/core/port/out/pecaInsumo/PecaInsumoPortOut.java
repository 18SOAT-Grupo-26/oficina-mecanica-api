package br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PecaInsumoPortOut {

    PecaInsumo salvar(PecaInsumo pecaInsumo);

    boolean reservar(Long id, int quantidade);

    boolean liberar(Long id, int quantidade);

    boolean baixar(Long id, int quantidade);

    Optional<PecaInsumo> buscarPorId(Long id);

    List<PecaInsumo> buscarPorIds(Collection<Long> ids);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    void excluir(PecaInsumo pecaInsumo);

    Pagina<PecaInsumo> listar(int pagina, int tamanho);
}
