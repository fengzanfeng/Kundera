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
package com.impetus.kundera.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;


/**
 * The Class PersistenceXMLLoader.
 *
 * @author amresh.singh
 */
public class PersistenceXMLLoader
{
    /** The log. */
    private static Log log = LogFactory.getLog(PersistenceXMLLoader.class);

    /**
     * Instantiates a new persistence xml loader.
     */
    private PersistenceXMLLoader()
    {
    }

    /**
     * Gets the document.
     * 
     * @param configURL
     *            the config url
     * @return the document
     * @throws Exception
     *             the exception
     */
    private static Document getDocument(URL configURL) throws Exception
    {
        InputStream is = null;
        if (configURL != null)
        {
            URLConnection conn = configURL.openConnection();
            conn.setUseCaches(false); // avoid JAR locking on Windows and Tomcat
            is = conn.getInputStream();
        }
        if (is == null)
        {
            throw new IOException("Failed to obtain InputStream from url: " + configURL);
        }

        DocumentBuilderFactory docBuilderFactory = null;
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(true);
        docBuilderFactory.setNamespaceAware(true);

        try
        {
            // otherwise Xerces fails in validation
            docBuilderFactory.setAttribute("http://apache.org/xml/features/validation/schema", true);
        }
        catch (IllegalArgumentException e)
        {
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setNamespaceAware(false);
        }

        InputSource source = new InputSource(is);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        // docBuilder.setEntityResolver( resolver );

        List errors = new ArrayList();
        docBuilder.setErrorHandler(new ErrorLogger("XML InputStream", errors));
        Document doc = docBuilder.parse(source);

        if (errors.size() != 0)
        {
            throw new PersistenceException("invalid persistence.xml", (Throwable) errors.get(0));
        }
        is.close(); // Close input Stream
        return doc;
    }

    /**
     * Find persistence units.
     * 
     * @param url
     *            the url
     * @return the list
     * @throws Exception
     *             the exception
     */
    public static List<PersistenceUnitMetadata> findPersistenceUnits(URL url) throws Exception
    {
        return findPersistenceUnits(url, PersistenceUnitTransactionType.JTA);
    }

    /**
     * Find persistence units.
     * 
     * @param url
     *            the url
     * @param defaultTransactionType
     *            the default transaction type
     * @return the list
     * @throws Exception
     *             the exception
     */
    public static List<PersistenceUnitMetadata> findPersistenceUnits(URL url,
            PersistenceUnitTransactionType defaultTransactionType) throws Exception
    {

        Document doc = getDocument(url);
        Element top = doc.getDocumentElement();
        NodeList children = top.getChildNodes();
        ArrayList<PersistenceUnitMetadata> units = new ArrayList<PersistenceUnitMetadata>();

        for (int i = 0; i < children.getLength(); i++)
        {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) children.item(i);
                String tag = element.getTagName();
                // look for "persistence-unit" element
                if (tag.equals("persistence-unit"))
                {
                    PersistenceUnitMetadata metadata = parsePersistenceUnit(element);
                    units.add(metadata);
                }
            }
        }
        return units;
    }

    /**
     * Parses the persistence unit.
     * 
     * @param top
     *            the top
     * @return the persistence metadata
     * @throws Exception
     *             the exception
     */
    private static PersistenceUnitMetadata parsePersistenceUnit(Element top) throws Exception
    {
        PersistenceUnitMetadata metadata = new PersistenceUnitMetadata();

        String puName = top.getAttribute("name");
        if (!isEmpty(puName))
        {
            log.trace("Persistent Unit name from persistence.xml: " + puName);
            metadata.setPersistenceUnitName(puName);
        }

        NodeList children = top.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) children.item(i);
                String tag = element.getTagName();

                if (tag.equals("provider"))
                {
                    metadata.setProvider(getElementContent(element));
                }
                else if (tag.equals("properties"))
                {
                    NodeList props = element.getChildNodes();
                    for (int j = 0; j < props.getLength(); j++)
                    {
                        if (props.item(j).getNodeType() == Node.ELEMENT_NODE)
                        {
                            Element propElement = (Element) props.item(j);
                            // if element is not "property" then skip
                            if (!"property".equals(propElement.getTagName()))
                            {
                                continue;
                            }

                            String propName = propElement.getAttribute("name").trim();
                            String propValue = propElement.getAttribute("value").trim();
                            if (isEmpty(propValue))
                            {
                                propValue = getElementContent(propElement, "");
                            }
                            metadata.getProperties().put(propName, propValue);
                        }
                    }
                }
                // Kundera doesn't support "class", "jar-file" and
                // "excluded-unlisted-classes" for now.. but will someday.
                // let's parse it for now.
                else if (tag.equals("class"))
                {
                    metadata.getClasses().add(getElementContent(element));
                }
                else if (tag.equals("jar-file"))
                {
                    metadata.getJarFiles().add(getElementContent(element));
                }
                else if (tag.equals("exclude-unlisted-classes"))
                {
                    metadata.setExcludeUnlistedClasses(true);
                }
            }
        }
        PersistenceUnitTransactionType transactionType = getTransactionType(top.getAttribute("transaction-type"));
        if (transactionType != null)
        {
            metadata.setTransactionType(transactionType);
        }

        return metadata;
    }

    /**
     * Gets the transaction type.
     * 
     * @param elementContent
     *            the element content
     * @return the transaction type
     */
    public static PersistenceUnitTransactionType getTransactionType(String elementContent)
    {

        if (elementContent == null || elementContent.isEmpty())
        {
            return null;
        }
        else if (elementContent.equalsIgnoreCase("JTA"))
        {
            return PersistenceUnitTransactionType.JTA;
        }
        else if (elementContent.equalsIgnoreCase("RESOURCE_LOCAL"))
        {
            return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }
        else
        {
            throw new PersistenceException("Unknown TransactionType: " + elementContent);
        }
    }

    /**
     * The Class ErrorLogger.
     */
    public static class ErrorLogger implements ErrorHandler
    {

        /** The file. */
        private String file;

        /** The errors. */
        private List errors;

        /**
         * Instantiates a new error logger.
         * 
         * @param file
         *            the file
         * @param errors
         *            the errors
         */
        ErrorLogger(String file, List errors)
        {
            this.file = file;
            this.errors = errors;
        }

        /* @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException) */
        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        public void error(SAXParseException error)
        {
            log.error("Error parsing XML: " + file + '(' + error.getLineNumber() + ") " + error.getMessage());
            errors.add(error);
        }

        /*
         * @see
         * org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        public void fatalError(SAXParseException error)
        {
            log.error("Error parsing XML: " + file + '(' + error.getLineNumber() + ") " + error.getMessage());
            errors.add(error);
        }

        /* @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException) */
        /* (non-Javadoc)
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        public void warning(SAXParseException warn)
        {
            log.warn("Warning parsing XML: " + file + '(' + warn.getLineNumber() + ") " + warn.getMessage());
        }
    }

    /**
     * Checks if is empty.
     * 
     * @param str
     *            the str
     * @return true, if is empty
     */
    private static boolean isEmpty(String str)
    {
        return null == str || str.isEmpty();
    }

    /**
     * Gets the element content.
     * 
     * @param element
     *            the element
     * @return the element content
     * @throws Exception
     *             the exception
     */
    public static String getElementContent(final Element element) throws Exception
    {
        return getElementContent(element, null);
    }

    /**
     * Get the content of the given element.
     * 
     * @param element
     *            The element to get the content for.
     * @param defaultStr
     *            The default to return when there is no content.
     * @return The content of the element or the default.
     * @throws Exception
     *             the exception
     */
    private static String getElementContent(Element element, String defaultStr) throws Exception
    {
        if (element == null)
        {
            return defaultStr;
        }

        NodeList children = element.getChildNodes();
        StringBuilder result = new StringBuilder("");
        for (int i = 0; i < children.getLength(); i++)
        {
            if (children.item(i).getNodeType() == Node.TEXT_NODE
                    || children.item(i).getNodeType() == Node.CDATA_SECTION_NODE)
            {
                result.append(children.item(i).getNodeValue());
            }
        }
        return result.toString().trim();
    }
}
