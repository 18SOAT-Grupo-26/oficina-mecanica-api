package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Feito à mão, e não com MapStruct, porque a OS não expõe setters e a gravação sincroniza a
 * entidade já carregada em vez de criar outra: itens que saíram do agregado são removidos, itens
 * novos (sem id) são incluídos e o histórico, que só cresce, recebe apenas os registros que faltam.
 */
@Component
public class OrdemServicoPersistenceMapper {

    public OrdemServicoEntity toEntity(OrdemServico os) {
        OrdemServicoEntity entity = new OrdemServicoEntity();
        copiar(os, entity);
        return entity;
    }

    public void copiar(OrdemServico os, OrdemServicoEntity entity) {
        entity.setClienteId(os.getIdCliente());
        entity.setVeiculoId(os.getIdVeiculo());
        entity.setStatus(os.getStatus());
        sincronizarServicos(os.getItensServico(), entity);
        sincronizarPecas(os.getItensPeca(), entity);
        acrescentarHistorico(os.getHistorico(), entity);
    }

    public OrdemServico toDomain(OrdemServicoEntity entity) {
        List<ItemServico> itensServico = entity.getItensServico().stream()
                .map(item -> ItemServico.reconstituir(item.getId(), item.getServicoId(), item.getDescricao(),
                        new ValorMonetario(item.getPrecoUnitario()), item.getQuantidade()))
                .toList();
        List<ItemPeca> itensPeca = entity.getItensPeca().stream()
                .map(item -> ItemPeca.reconstituir(item.getId(), item.getPecaInsumoId(), item.getDescricao(),
                        new ValorMonetario(item.getPrecoUnitario()), item.getQuantidade()))
                .toList();
        List<RegistroHistorico> historico = entity.getHistorico().stream()
                .map(registro -> new RegistroHistorico(registro.getStatusAntigo(), registro.getStatusAtualizado(),
                        registro.getDataCriacao()))
                .toList();
        return OrdemServico.reconstituir(entity.getId(), entity.getClienteId(), entity.getVeiculoId(),
                entity.getStatus(), itensServico, itensPeca, historico);
    }

    private static void sincronizarServicos(List<ItemServico> itens, OrdemServicoEntity entity) {
        Set<Long> mantidos = idsPersistidos(itens.stream().map(ItemServico::getId).toList());
        entity.getItensServico().removeIf(item -> !mantidos.contains(item.getId()));
        itens.stream().filter(item -> item.getId() == null).forEach(item -> {
            ItemServicoOsEntity novo = new ItemServicoOsEntity();
            novo.setOrdemServico(entity);
            novo.setServicoId(item.getIdServico());
            novo.setDescricao(item.getDescricao());
            novo.setPrecoUnitario(item.getPrecoUnitario().valor());
            novo.setQuantidade(item.getQuantidade());
            entity.getItensServico().add(novo);
        });
    }

    private static void sincronizarPecas(List<ItemPeca> itens, OrdemServicoEntity entity) {
        Set<Long> mantidos = idsPersistidos(itens.stream().map(ItemPeca::getId).toList());
        entity.getItensPeca().removeIf(item -> !mantidos.contains(item.getId()));
        itens.stream().filter(item -> item.getId() == null).forEach(item -> {
            ItemPecaOsEntity novo = new ItemPecaOsEntity();
            novo.setOrdemServico(entity);
            novo.setPecaInsumoId(item.getIdPeca());
            novo.setDescricao(item.getDescricao());
            novo.setPrecoUnitario(item.getPrecoUnitario().valor());
            novo.setQuantidade(item.getQuantidade());
            entity.getItensPeca().add(novo);
        });
    }

    private static void acrescentarHistorico(List<RegistroHistorico> historico, OrdemServicoEntity entity) {
        historico.stream().skip(entity.getHistorico().size()).forEach(registro -> {
            HistoricoStatusOsEntity novo = new HistoricoStatusOsEntity();
            novo.setOrdemServico(entity);
            novo.setStatusAntigo(registro.statusAntigo());
            novo.setStatusAtualizado(registro.statusAtualizado());
            novo.setDataCriacao(registro.dataHora());
            entity.getHistorico().add(novo);
        });
    }

    private static Set<Long> idsPersistidos(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
