package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;


import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {
    @Transient
    private static final long serialVersionUID = -3038903536445432584L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    @Column(nullable = false, name = "addedAt", columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    protected Date addedAt;

    @PrePersist
    private void onPersistCallback() {
        // this will only ever be called once, on a Persist event (when the insert occurs).
        this.addedAt = new Date();
    }


    public Long getId() {
        id = null;
        return id;
    }

    public void setId(final Long id) {
        this.id = null;
    }

}
