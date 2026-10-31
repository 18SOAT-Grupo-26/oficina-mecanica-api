package br.com.fiap.oficinamecanicaapi.core.port.out.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PecaInsumoPortOut {

    PecaInsumo salvar(PecaInsumo pecaInsumo);

    boolean reservar(Long id, int quantidade);

    boolean liberar(Long id, int quantidade);

    boolean baixar(Long id, int quantidade);

    Optional<PecaInsumo> buscarAtivoPorId(Long id);

    List<PecaInsumo> buscarPorIds(Collection<Long> ids);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    Pagina<PecaInsumo> listarAtivos(int pagina, int tamanho);
}
