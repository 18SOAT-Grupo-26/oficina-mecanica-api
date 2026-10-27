package br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PecaInsumoPortOut {

    PecaInsumoEntity salvar(PecaInsumoEntity pecaInsumo);

    boolean reservar(Long id, int quantidade);

    boolean liberar(Long id, int quantidade);

    boolean baixar(Long id, int quantidade);

    Optional<PecaInsumoEntity> buscarPorId(Long id);

    List<PecaInsumoEntity> buscarPorIds(Collection<Long> ids);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    void excluir(PecaInsumoEntity pecaInsumo);

    Pagina<PecaInsumoEntity> listar(int pagina, int tamanho);
}
