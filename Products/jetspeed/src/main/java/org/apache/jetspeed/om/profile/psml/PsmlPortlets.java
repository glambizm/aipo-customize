/*
 * Copyright 2000-2001,2004 The Apache Software Foundation.
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

package org.apache.jetspeed.om.profile.psml;

// Java imports
import java.util.Iterator;
import java.util.Vector;

import org.apache.jetspeed.om.SecurityReference;
import org.apache.jetspeed.om.profile.Controller;
import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Layout;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Reference;
import org.apache.jetspeed.om.profile.Security;

/**
 * Base simple bean-like implementation of the Portlets interface suitable for
 * Castor XML serialization.
 * 
 * @author <a href="mailto:taylor@apache.org">David Sean Taylor</a>
 */
public class PsmlPortlets extends PsmlIdentityElement implements Portlets,
    java.io.Serializable {
  private Controller controller = null;

  private Security security = null;

  private Vector portlets = new Vector();

  private Vector entries = new Vector();

  private Vector refs = new Vector();

  /** Holds value of property securityRef. */
  private SecurityReference securityRef = null;

  private Portlets parentPortlets;

  public PsmlPortlets() {
  }

  public Controller getController() {
    return this.controller;
  }

  public void setController(Controller controller) {
    this.controller = controller;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Security getSecurity() {
    return this.security;
  }

  public Vector getEntries() {
    return this.entries;
  }

  public void setEntries(Vector entries) {
    this.entries = entries;
  }

  /**
   * Return a list of portlet. Portlets that where added via a reference, see
   * addReference(), are excluded.
   * 
   * @return Vector of portlet
   */
  public Vector getPortlets() {
    Vector v = new Vector();
    for (int ix = 0; ix < this.portlets.size(); ix++) {
      Portlets p = (Portlets) this.portlets.get(ix);
      if (p instanceof Reference) {
        // Do not want to include portlets that where added via reference
        continue;
      }
      v.add(p);
    }
    return v;
  }

  public void setPortlets(Vector portlets) {
    this.portlets = portlets;
  }

  public Vector getReferences() {
    return this.refs;
  }

  public void addPortlets(PsmlPortlets p) {
    portlets.addElement(p);
  }

  public void addReference(PsmlReference ref) {
    this.refs.addElement(ref);
    portlets.addElement(ref);
  }

  public void addReference(Reference ref)
      throws java.lang.IndexOutOfBoundsException {
    this.refs.addElement(ref);
    portlets.addElement(ref);
  }

  public int getEntryCount() {
    return this.entries.size();
  }

  public int getReferenceCount() {
    return this.refs.size();
  }

  public int getPortletsCount() {
    return this.portlets.size();
  }

  public Entry removeEntry(int index) {
    Object obj = entries.elementAt(index);
    entries.removeElementAt(index);
    return (Entry) obj;
  }

  public Portlets removePortlets(int index) {
    Object obj = portlets.elementAt(index);
    if (null == obj) {
      return (Portlets) obj;
    }

    portlets.removeElementAt(index);
    if (obj instanceof Reference) {
      refs.remove(obj);
    }
    return (Portlets) obj;
  }

  public Reference removeReference(int index) {
    Object obj = refs.elementAt(index);
    refs.removeElementAt(index);
    portlets.remove(obj);
    return (Reference) obj;
  }

  public Entry getEntry(int index) throws java.lang.IndexOutOfBoundsException {
    // -- check bounds for index
    if ((index < 0) || (index > entries.size())) {
      throw new IndexOutOfBoundsException();
    }

    return (Entry) entries.elementAt(index);
  }

  public Portlets getPortlets(int index)
      throws java.lang.IndexOutOfBoundsException {
    // -- check bounds for index
    if ((index < 0) || (index > portlets.size())) {
      throw new IndexOutOfBoundsException();
    }

    return (Portlets) portlets.elementAt(index);
  }

  public Reference getReference(int index)
      throws java.lang.IndexOutOfBoundsException {
    if ((index < 0) || (index > refs.size())) {
      throw new IndexOutOfBoundsException();
    }

    return (Reference) refs.elementAt(index);
  }

  public Iterator getEntriesIterator() {
    return entries.iterator();
  }

  public Iterator getPortletsIterator() {
    return portlets.iterator();
  }

  public Iterator getReferenceIterator() {
    return refs.iterator();
  }

  public void addEntry(Entry entry) throws java.lang.IndexOutOfBoundsException {
    entries.addElement(entry);
  }

  public void addPortlets(Portlets p)
      throws java.lang.IndexOutOfBoundsException {
    portlets.addElement(p);
    // STW make sure layout gets set
    int end = getEntryCount();
    Layout layout = p.getLayout();

    // if (layout != null) {
    // layout.setPosition(end);
    // layout.setSize(-1);
    // }

    // 理由等：レイアウトの指定がない場合に，
    // 新規レイアウトを追加可能にした．
    if (layout == null) {
      layout = new PsmlLayout();
      layout.setPosition(end);
      layout.setSize(-1);
      p.setLayout(layout);
    }
  }

  public Entry[] getEntriesArray() {
    int size = entries.size();
    Entry[] mArray = new Entry[size];
    for (int index = 0; index < size; index++) {
      mArray[index] = (Entry) entries.elementAt(index);
    }
    return mArray;
  }

  public Portlets[] getPortletsArray() {
    int size = portlets.size();
    Portlets[] mArray = new Portlets[size];
    for (int index = 0; index < size; index++) {
      mArray[index] = (Portlets) portlets.elementAt(index);
    }
    return mArray;
  }

  public Reference[] getReferenceArray() {
    int size = refs.size();
    Reference[] mArray = new Reference[size];
    for (int index = 0; index < size; index++) {
      mArray[index] = (Reference) refs.elementAt(index);
    }
    return mArray;
  }

  /**
   * Getter for property securityRef.
   * 
   * @return Value of property securityRef.
   */
  public SecurityReference getSecurityRef() {
    return securityRef;
  }

  /**
   * Setter for property securityRef.
   * 
   * @param securityRef
   *            New value of property securityRef.
   */
  public void setSecurityRef(SecurityReference securityRef) {
    this.securityRef = securityRef;
  }

  /**
   * Create a clone of this object
   */
  public Object clone() throws java.lang.CloneNotSupportedException {
    Object cloned = super.clone();

    ((PsmlPortlets) cloned).controller = ((this.controller == null) ? null
        : (Controller) this.controller.clone());
    ((PsmlPortlets) cloned).security = ((this.security == null) ? null
        : (Security) this.security.clone());

    if (this.portlets != null) {
      ((PsmlPortlets) cloned).portlets = new Vector(this.portlets.size());
      Iterator it = this.portlets.iterator();
      while (it.hasNext()) {
        ((PsmlPortlets) cloned).portlets.add(((Portlets) it.next()).clone());
      }
    }

    if (this.entries != null) {
      ((PsmlPortlets) cloned).entries = new Vector(this.entries.size());
      Iterator it = this.entries.iterator();
      while (it.hasNext()) {
        ((PsmlPortlets) cloned).entries.add(((Entry) it.next()).clone());
      }
    }

    if (this.refs != null) {
      ((PsmlPortlets) cloned).refs = new Vector(this.refs.size());
      Iterator it = this.refs.iterator();
      while (it.hasNext()) {
        ((PsmlPortlets) cloned).refs.add(((Reference) it.next()).clone());
      }
    }

    ((PsmlPortlets) cloned).securityRef = ((this.securityRef == null) ? null
        : (SecurityReference) this.securityRef.clone());

    return cloned;

  } // clone

  /**
   * Returns the parent.
   * 
   * @return Portlets
   */
  public Portlets getParentPortlets() {
    return parentPortlets;
  }

  /**
   * Sets the parent.
   * 
   * @param parent
   *            The parent to set
   */
  public void setParentPortlets(Portlets parent) {
    this.parentPortlets = parent;
  }

  /**
   * @see org.apache.jetspeed.om.profile.IdentityElement#getSkin()
   */
  // public Skin getSkin()
  // {
  // Skin useSkin = super.getSkin();
  // if(useSkin == null && parentPortlets != null)
  // {
  // useSkin = parentPortlets.getSkin();
  // }
  //        
  // return useSkin;
  // }
}
