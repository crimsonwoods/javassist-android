/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.scopedpool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Map will remove entries when the value in the map has been cleaned from
 * garbage collection
 * 
 * @version <tt>$Revision: 1.4 $</tt>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class SoftValueHashMap<K, V> implements Map<K, V> {
    private static class SoftValueRef<K1, V1> extends SoftReference<V1> {
        public K1 key;

        private SoftValueRef(K1 key, V1 val, ReferenceQueue<V1> q) {
            super(val, q);
            this.key = key;
        }
    }
    
    private SoftValueRef<K, V> create(K key, V val,
            ReferenceQueue<V> q) {
        if (val == null)
            return null;
        else
            return new SoftValueRef<K, V>(key, val, q);
    }

    private static class InnerEntry<K1, V1> implements Map.Entry<K1, V1> {
    	private K1 key;
    	private V1 value;
    	
    	private InnerEntry(K1 k, V1 v) {
    		this.key = k;
    		this.value = v;
    	}
		@Override
		public K1 getKey() {
			return key;
		}

		@Override
		public V1 getValue() {
			return value;
		}

		@Override
		public V1 setValue(V1 object) {
			V1 old = this.value;
			this.value = object;
			return old;
		}
    }
    
    /**
     * Returns a set of the mappings contained in this hash table.
     */
    public Set<Entry<K, V>> entrySet() {
        processQueue();
        final HashSet<Entry<K, V>> set = new HashSet<Entry<K, V>>();
        for (Entry<K, SoftValueRef<K, V>> e : hash.entrySet()) {
        	set.add(new InnerEntry<K, V>(e.getKey(), e.getValue().get()));
        }
        return set;
    }

    /* Hash table mapping WeakKeys to values */
    private Map<K, SoftValueRef<K, V>> hash;

    /* Reference queue for cleared WeakKeys */
    private ReferenceQueue<V> queue = new ReferenceQueue<V>();

    /*
     * Remove all invalidated entries from the map, that is, remove all entries
     * whose values have been discarded.
     */
    @SuppressWarnings("unchecked")
	private void processQueue() {
        SoftValueRef<K, V> ref;
        while ((ref = (SoftValueRef<K, V>)queue.poll()) != null) {
            if (ref == hash.get(ref.key)) {
                // only remove if it is the *exact* same WeakValueRef
                //
                hash.remove(ref.key);
            }
        }
    }

    /* -- Constructors -- */

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given initial
     * capacity and the given load factor.
     * 
     * @param initialCapacity
     *            The initial capacity of the <code>WeakHashMap</code>
     * 
     * @param loadFactor
     *            The load factor of the <code>WeakHashMap</code>
     * 
     * @throws IllegalArgumentException
     *             If the initial capacity is less than zero, or if the load
     *             factor is nonpositive
     */
    public SoftValueHashMap(int initialCapacity, float loadFactor) {
        hash = new HashMap<K, SoftValueRef<K, V>>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given initial
     * capacity and the default load factor, which is <code>0.75</code>.
     * 
     * @param initialCapacity
     *            The initial capacity of the <code>WeakHashMap</code>
     * 
     * @throws IllegalArgumentException
     *             If the initial capacity is less than zero
     */
    public SoftValueHashMap(int initialCapacity) {
        hash = new HashMap<K, SoftValueRef<K, V>>(initialCapacity);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * initial capacity and the default load factor, which is <code>0.75</code>.
     */
    public SoftValueHashMap() {
        hash = new HashMap<K, SoftValueRef<K, V>>();
    }

    /**
     * Constructs a new <code>WeakHashMap</code> with the same mappings as the
     * specified <tt>Map</tt>. The <code>WeakHashMap</code> is created with
     * an initial capacity of twice the number of mappings in the specified map
     * or 11 (whichever is greater), and a default load factor, which is
     * <tt>0.75</tt>.
     * 
     * @param t     the map whose mappings are to be placed in this map.
     */
    public SoftValueHashMap(Map<K, V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    /* -- Simple queries -- */

    /**
     * Returns the number of key-value mappings in this map. <strong>Note:</strong>
     * <em>In contrast with most implementations of the
     * <code>Map</code> interface, the time required by this operation is
     * linear in the size of the map.</em>
     */
    public int size() {
        processQueue();
        return hash.size();
    }

    /**
     * Returns <code>true</code> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        processQueue();
        return hash.isEmpty();
    }

    /**
     * Returns <code>true</code> if this map contains a mapping for the
     * specified key.
     * 
     * @param key
     *            The key whose presence in this map is to be tested.
     */
    public boolean containsKey(Object key) {
        processQueue();
        return hash.containsKey(key);
    }

    /* -- Lookup and modification operations -- */

    /**
     * Returns the value to which this map maps the specified <code>key</code>.
     * If this map does not contain a value for this key, then return
     * <code>null</code>.
     * 
     * @param key
     *            The key whose associated value, if any, is to be returned.
     */
	public V get(Object key) {
        processQueue();
        SoftReference<V> ref = (SoftReference<V>)hash.get(key);
        if (ref != null)
            return ref.get();
        return null;
    }

    /**
     * Updates this map so that the given <code>key</code> maps to the given
     * <code>value</code>. If the map previously contained a mapping for
     * <code>key</code> then that mapping is replaced and the previous value
     * is returned.
     * 
     * @param key
     *            The key that is to be mapped to the given <code>value</code>
     * @param value
     *            The value to which the given <code>key</code> is to be
     *            mapped
     * 
     * @return The previous value to which this key was mapped, or
     *         <code>null</code> if if there was no mapping for the key
     */
	public V put(K key, V value) {
        processQueue();
        SoftValueRef<K, V> rtn = hash.put(key, create(key, value, queue));
        if (rtn != null)
            return rtn.get();
        return null;
    }

    /**
     * Removes the mapping for the given <code>key</code> from this map, if
     * present.
     * 
     * @param key
     *            The key whose mapping is to be removed.
     * 
     * @return The value to which this key was mapped, or <code>null</code> if
     *         there was no mapping for the key.
     */
    public V remove(Object key) {
        processQueue();
        SoftValueRef<K, V> ref = hash.remove(key);
        if (null != ref)
        	return ref.get();
        return null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        processQueue();
        hash.clear();
    }

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof SoftValueRef) {
			return hash.containsValue(value);
		}
		for (SoftValueRef<K, V> ref : hash.values()) {
			if (ref.get() == value) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<K> keySet() {
		return hash.keySet();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> arg0) {
		for (Entry<? extends K,? extends V> e : arg0.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	@Override
	public Collection<V> values() {
		final ArrayList<V> set = new ArrayList<V>();
		for (SoftValueRef<K, V> v : hash.values()) {
			set.add(v.get());
		}
		return set;
	}
}
