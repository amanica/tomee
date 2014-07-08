/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openejb.jee.jpa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * @Target({METHOD, FIELD}) @Retention(RUNTIME)
 * public @interface JoinTable {
 * String name() default "";
 * String catalog() default "";
 * String schema() default "";
 * JoinColumn[] joinColumns() default {};
 * JoinColumn[] inverseJoinColumns() default {};
 * UniqueConstraint[] uniqueConstraints() default {};
 * }
 * <p/>
 * <p/>
 * <p/>
 * <p>Java class for join-table complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="join-table">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="join-column" type="{http://java.sun.com/xml/ns/persistence/orm}join-column" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="inverse-join-column" type="{http://java.sun.com/xml/ns/persistence/orm}join-column" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="unique-constraint" type="{http://java.sun.com/xml/ns/persistence/orm}unique-constraint" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="catalog" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="schema" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "join-table", propOrder = {
    "joinColumn",
    "inverseJoinColumn",
    "uniqueConstraint"
})
public class JoinTable {

    @XmlElement(name = "join-column")
    protected List<JoinColumn> joinColumn;
    @XmlElement(name = "inverse-join-column")
    protected List<JoinColumn> inverseJoinColumn;
    @XmlElement(name = "unique-constraint")
    protected List<UniqueConstraint> uniqueConstraint;
    @XmlAttribute
    protected String catalog;
    @XmlAttribute
    protected String name;
    @XmlAttribute
    protected String schema;

    /**
     * Gets the value of the joinColumn property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the joinColumn property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJoinColumn().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link JoinColumn }
     */
    public List<JoinColumn> getJoinColumn() {
        if (joinColumn == null) {
            joinColumn = new ArrayList<JoinColumn>();
        }
        return this.joinColumn;
    }

    /**
     * Gets the value of the inverseJoinColumn property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inverseJoinColumn property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInverseJoinColumn().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link JoinColumn }
     */
    public List<JoinColumn> getInverseJoinColumn() {
        if (inverseJoinColumn == null) {
            inverseJoinColumn = new ArrayList<JoinColumn>();
        }
        return this.inverseJoinColumn;
    }

    /**
     * Gets the value of the uniqueConstraint property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the uniqueConstraint property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUniqueConstraint().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link UniqueConstraint }
     */
    public List<UniqueConstraint> getUniqueConstraint() {
        if (uniqueConstraint == null) {
            uniqueConstraint = new ArrayList<UniqueConstraint>();
        }
        return this.uniqueConstraint;
    }

    /**
     * Gets the value of the catalog property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Sets the value of the catalog property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCatalog(final String value) {
        this.catalog = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(final String value) {
        this.name = value;
    }

    /**
     * Gets the value of the schema property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the value of the schema property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSchema(final String value) {
        this.schema = value;
    }

}
