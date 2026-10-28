package br.com.fiap.oficinamecanicaapi.shared.persistence;

import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Datas de criação e de última alteração das tabelas com created_at/updated_at,
 * preenchidas pelo JPA ao inserir e ao atualizar.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class EntidadeComDatas {

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime agora = GetLocalTime.getLocalTime();
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = GetLocalTime.getLocalTime();
    }
}