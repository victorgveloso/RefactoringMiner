package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;


import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

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
