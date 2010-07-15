/*
 * Copyright 2010 Impetus Infotech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.impetus.kundera;

import java.util.List;

import javax.persistence.EntityManager;

import com.impetus.kundera.metadata.MetadataManager;

/**
 * The Interface CassandraEntityManager.
 * 
 * @author animesh.kumar
 */
public interface CassandraEntityManager extends EntityManager {

    /**
     * Find.
     * 
     * @param entityClass
     *            the entity class
     * @param primaryKey
     *            the primary key
     * 
     * @return the list< t>
     */
    public <T> List<T> find(Class<T> entityClass, Object... primaryKey);

    /**
     * Gets the metadata manager.
     * 
     * @return the metadata manager
     */
    MetadataManager getMetadataManager();

    /**
     * Gets the client.
     * 
     * @return the client
     */
    CassandraClient getClient();

}