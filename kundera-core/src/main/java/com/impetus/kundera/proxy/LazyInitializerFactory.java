/*******************************************************************************
 * * Copyright 2011 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.kundera.proxy;

import java.lang.reflect.Method;

import javax.persistence.PersistenceException;

import com.impetus.kundera.persistence.PersistenceDelegator;


/**
 * Interface LazyInitializerFactory
 * 
 * Creates Lazily loadable proxies for @Entity classes.
 * 
 * @author animesh.kumar
 */
public interface LazyInitializerFactory
{

    /**
     * Get Lazily loadable @Entity proxy.
     *
     * @param entityName the entity name
     * @param persistentClass the persistent class
     * @param getIdentifierMethod the get identifier method
     * @param setIdentifierMethod the set identifier method
     * @param id the id
     * @param persistenceDelegator the persistence delegator
     * @return the proxy
     * @throws PersistenceException the persistence exception
     */
    KunderaProxy getProxy(final String entityName, final Class<?> persistentClass, final Method getIdentifierMethod,
            final Method setIdentifierMethod, final String id, final PersistenceDelegator persistenceDelegator)
            throws PersistenceException;

}
