package br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

import java.util.Optional;

public interface PecaInsumoPortOut {

    PecaInsumoEntity salvar(PecaInsumoEntity pecaInsumo);

    Optional<PecaInsumoEntity> buscarPorId(Long id);

    boolean existePorNomeNormalizado(String nomeNormalizado);

    void excluir(PecaInsumoEntity pecaInsumo);

    Pagina<PecaInsumoEntity> listar(int pagina, int tamanho);
}
