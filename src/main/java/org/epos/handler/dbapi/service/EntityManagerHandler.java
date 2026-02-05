package org.epos.handler.dbapi.service;

import jakarta.persistence.EntityManager;

public interface EntityManagerHandler {
    EntityManager getEntityManager();
}