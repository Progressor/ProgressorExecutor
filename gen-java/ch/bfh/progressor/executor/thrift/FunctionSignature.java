/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package ch.bfh.progressor.executor.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)", date = "2015-12-10")
public class FunctionSignature implements org.apache.thrift.TBase<FunctionSignature, FunctionSignature._Fields>, java.io.Serializable, Cloneable, Comparable<FunctionSignature> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("FunctionSignature");

  private static final org.apache.thrift.protocol.TField NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("name", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField INPUT_NAMES_FIELD_DESC = new org.apache.thrift.protocol.TField("inputNames", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField INPUT_TYPES_FIELD_DESC = new org.apache.thrift.protocol.TField("inputTypes", org.apache.thrift.protocol.TType.LIST, (short)3);
  private static final org.apache.thrift.protocol.TField OUTPUT_NAMES_FIELD_DESC = new org.apache.thrift.protocol.TField("outputNames", org.apache.thrift.protocol.TType.LIST, (short)4);
  private static final org.apache.thrift.protocol.TField OUTPUT_TYPES_FIELD_DESC = new org.apache.thrift.protocol.TField("outputTypes", org.apache.thrift.protocol.TType.LIST, (short)5);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new FunctionSignatureStandardSchemeFactory());
    schemes.put(TupleScheme.class, new FunctionSignatureTupleSchemeFactory());
  }

  public String name; // required
  public List<String> inputNames; // required
  public List<String> inputTypes; // required
  public List<String> outputNames; // required
  public List<String> outputTypes; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    NAME((short)1, "name"),
    INPUT_NAMES((short)2, "inputNames"),
    INPUT_TYPES((short)3, "inputTypes"),
    OUTPUT_NAMES((short)4, "outputNames"),
    OUTPUT_TYPES((short)5, "outputTypes");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // NAME
          return NAME;
        case 2: // INPUT_NAMES
          return INPUT_NAMES;
        case 3: // INPUT_TYPES
          return INPUT_TYPES;
        case 4: // OUTPUT_NAMES
          return OUTPUT_NAMES;
        case 5: // OUTPUT_TYPES
          return OUTPUT_TYPES;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.NAME, new org.apache.thrift.meta_data.FieldMetaData("name", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.INPUT_NAMES, new org.apache.thrift.meta_data.FieldMetaData("inputNames", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.INPUT_TYPES, new org.apache.thrift.meta_data.FieldMetaData("inputTypes", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.OUTPUT_NAMES, new org.apache.thrift.meta_data.FieldMetaData("outputNames", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.OUTPUT_TYPES, new org.apache.thrift.meta_data.FieldMetaData("outputTypes", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(FunctionSignature.class, metaDataMap);
  }

  public FunctionSignature() {
  }

  public FunctionSignature(
    String name,
    List<String> inputNames,
    List<String> inputTypes,
    List<String> outputNames,
    List<String> outputTypes)
  {
    this();
    this.name = name;
    this.inputNames = inputNames;
    this.inputTypes = inputTypes;
    this.outputNames = outputNames;
    this.outputTypes = outputTypes;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public FunctionSignature(FunctionSignature other) {
    if (other.isSetName()) {
      this.name = other.name;
    }
    if (other.isSetInputNames()) {
      List<String> __this__inputNames = new ArrayList<String>(other.inputNames);
      this.inputNames = __this__inputNames;
    }
    if (other.isSetInputTypes()) {
      List<String> __this__inputTypes = new ArrayList<String>(other.inputTypes);
      this.inputTypes = __this__inputTypes;
    }
    if (other.isSetOutputNames()) {
      List<String> __this__outputNames = new ArrayList<String>(other.outputNames);
      this.outputNames = __this__outputNames;
    }
    if (other.isSetOutputTypes()) {
      List<String> __this__outputTypes = new ArrayList<String>(other.outputTypes);
      this.outputTypes = __this__outputTypes;
    }
  }

  public FunctionSignature deepCopy() {
    return new FunctionSignature(this);
  }

  @Override
  public void clear() {
    this.name = null;
    this.inputNames = null;
    this.inputTypes = null;
    this.outputNames = null;
    this.outputTypes = null;
  }

  public String getName() {
    return this.name;
  }

  public FunctionSignature setName(String name) {
    this.name = name;
    return this;
  }

  public void unsetName() {
    this.name = null;
  }

  /** Returns true if field name is set (has been assigned a value) and false otherwise */
  public boolean isSetName() {
    return this.name != null;
  }

  public void setNameIsSet(boolean value) {
    if (!value) {
      this.name = null;
    }
  }

  public int getInputNamesSize() {
    return (this.inputNames == null) ? 0 : this.inputNames.size();
  }

  public java.util.Iterator<String> getInputNamesIterator() {
    return (this.inputNames == null) ? null : this.inputNames.iterator();
  }

  public void addToInputNames(String elem) {
    if (this.inputNames == null) {
      this.inputNames = new ArrayList<String>();
    }
    this.inputNames.add(elem);
  }

  public List<String> getInputNames() {
    return this.inputNames;
  }

  public FunctionSignature setInputNames(List<String> inputNames) {
    this.inputNames = inputNames;
    return this;
  }

  public void unsetInputNames() {
    this.inputNames = null;
  }

  /** Returns true if field inputNames is set (has been assigned a value) and false otherwise */
  public boolean isSetInputNames() {
    return this.inputNames != null;
  }

  public void setInputNamesIsSet(boolean value) {
    if (!value) {
      this.inputNames = null;
    }
  }

  public int getInputTypesSize() {
    return (this.inputTypes == null) ? 0 : this.inputTypes.size();
  }

  public java.util.Iterator<String> getInputTypesIterator() {
    return (this.inputTypes == null) ? null : this.inputTypes.iterator();
  }

  public void addToInputTypes(String elem) {
    if (this.inputTypes == null) {
      this.inputTypes = new ArrayList<String>();
    }
    this.inputTypes.add(elem);
  }

  public List<String> getInputTypes() {
    return this.inputTypes;
  }

  public FunctionSignature setInputTypes(List<String> inputTypes) {
    this.inputTypes = inputTypes;
    return this;
  }

  public void unsetInputTypes() {
    this.inputTypes = null;
  }

  /** Returns true if field inputTypes is set (has been assigned a value) and false otherwise */
  public boolean isSetInputTypes() {
    return this.inputTypes != null;
  }

  public void setInputTypesIsSet(boolean value) {
    if (!value) {
      this.inputTypes = null;
    }
  }

  public int getOutputNamesSize() {
    return (this.outputNames == null) ? 0 : this.outputNames.size();
  }

  public java.util.Iterator<String> getOutputNamesIterator() {
    return (this.outputNames == null) ? null : this.outputNames.iterator();
  }

  public void addToOutputNames(String elem) {
    if (this.outputNames == null) {
      this.outputNames = new ArrayList<String>();
    }
    this.outputNames.add(elem);
  }

  public List<String> getOutputNames() {
    return this.outputNames;
  }

  public FunctionSignature setOutputNames(List<String> outputNames) {
    this.outputNames = outputNames;
    return this;
  }

  public void unsetOutputNames() {
    this.outputNames = null;
  }

  /** Returns true if field outputNames is set (has been assigned a value) and false otherwise */
  public boolean isSetOutputNames() {
    return this.outputNames != null;
  }

  public void setOutputNamesIsSet(boolean value) {
    if (!value) {
      this.outputNames = null;
    }
  }

  public int getOutputTypesSize() {
    return (this.outputTypes == null) ? 0 : this.outputTypes.size();
  }

  public java.util.Iterator<String> getOutputTypesIterator() {
    return (this.outputTypes == null) ? null : this.outputTypes.iterator();
  }

  public void addToOutputTypes(String elem) {
    if (this.outputTypes == null) {
      this.outputTypes = new ArrayList<String>();
    }
    this.outputTypes.add(elem);
  }

  public List<String> getOutputTypes() {
    return this.outputTypes;
  }

  public FunctionSignature setOutputTypes(List<String> outputTypes) {
    this.outputTypes = outputTypes;
    return this;
  }

  public void unsetOutputTypes() {
    this.outputTypes = null;
  }

  /** Returns true if field outputTypes is set (has been assigned a value) and false otherwise */
  public boolean isSetOutputTypes() {
    return this.outputTypes != null;
  }

  public void setOutputTypesIsSet(boolean value) {
    if (!value) {
      this.outputTypes = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case NAME:
      if (value == null) {
        unsetName();
      } else {
        setName((String)value);
      }
      break;

    case INPUT_NAMES:
      if (value == null) {
        unsetInputNames();
      } else {
        setInputNames((List<String>)value);
      }
      break;

    case INPUT_TYPES:
      if (value == null) {
        unsetInputTypes();
      } else {
        setInputTypes((List<String>)value);
      }
      break;

    case OUTPUT_NAMES:
      if (value == null) {
        unsetOutputNames();
      } else {
        setOutputNames((List<String>)value);
      }
      break;

    case OUTPUT_TYPES:
      if (value == null) {
        unsetOutputTypes();
      } else {
        setOutputTypes((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case NAME:
      return getName();

    case INPUT_NAMES:
      return getInputNames();

    case INPUT_TYPES:
      return getInputTypes();

    case OUTPUT_NAMES:
      return getOutputNames();

    case OUTPUT_TYPES:
      return getOutputTypes();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case NAME:
      return isSetName();
    case INPUT_NAMES:
      return isSetInputNames();
    case INPUT_TYPES:
      return isSetInputTypes();
    case OUTPUT_NAMES:
      return isSetOutputNames();
    case OUTPUT_TYPES:
      return isSetOutputTypes();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof FunctionSignature)
      return this.equals((FunctionSignature)that);
    return false;
  }

  public boolean equals(FunctionSignature that) {
    if (that == null)
      return false;

    boolean this_present_name = true && this.isSetName();
    boolean that_present_name = true && that.isSetName();
    if (this_present_name || that_present_name) {
      if (!(this_present_name && that_present_name))
        return false;
      if (!this.name.equals(that.name))
        return false;
    }

    boolean this_present_inputNames = true && this.isSetInputNames();
    boolean that_present_inputNames = true && that.isSetInputNames();
    if (this_present_inputNames || that_present_inputNames) {
      if (!(this_present_inputNames && that_present_inputNames))
        return false;
      if (!this.inputNames.equals(that.inputNames))
        return false;
    }

    boolean this_present_inputTypes = true && this.isSetInputTypes();
    boolean that_present_inputTypes = true && that.isSetInputTypes();
    if (this_present_inputTypes || that_present_inputTypes) {
      if (!(this_present_inputTypes && that_present_inputTypes))
        return false;
      if (!this.inputTypes.equals(that.inputTypes))
        return false;
    }

    boolean this_present_outputNames = true && this.isSetOutputNames();
    boolean that_present_outputNames = true && that.isSetOutputNames();
    if (this_present_outputNames || that_present_outputNames) {
      if (!(this_present_outputNames && that_present_outputNames))
        return false;
      if (!this.outputNames.equals(that.outputNames))
        return false;
    }

    boolean this_present_outputTypes = true && this.isSetOutputTypes();
    boolean that_present_outputTypes = true && that.isSetOutputTypes();
    if (this_present_outputTypes || that_present_outputTypes) {
      if (!(this_present_outputTypes && that_present_outputTypes))
        return false;
      if (!this.outputTypes.equals(that.outputTypes))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_name = true && (isSetName());
    list.add(present_name);
    if (present_name)
      list.add(name);

    boolean present_inputNames = true && (isSetInputNames());
    list.add(present_inputNames);
    if (present_inputNames)
      list.add(inputNames);

    boolean present_inputTypes = true && (isSetInputTypes());
    list.add(present_inputTypes);
    if (present_inputTypes)
      list.add(inputTypes);

    boolean present_outputNames = true && (isSetOutputNames());
    list.add(present_outputNames);
    if (present_outputNames)
      list.add(outputNames);

    boolean present_outputTypes = true && (isSetOutputTypes());
    list.add(present_outputTypes);
    if (present_outputTypes)
      list.add(outputTypes);

    return list.hashCode();
  }

  @Override
  public int compareTo(FunctionSignature other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetName()).compareTo(other.isSetName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.name, other.name);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetInputNames()).compareTo(other.isSetInputNames());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetInputNames()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.inputNames, other.inputNames);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetInputTypes()).compareTo(other.isSetInputTypes());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetInputTypes()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.inputTypes, other.inputTypes);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetOutputNames()).compareTo(other.isSetOutputNames());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOutputNames()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.outputNames, other.outputNames);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetOutputTypes()).compareTo(other.isSetOutputTypes());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOutputTypes()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.outputTypes, other.outputTypes);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("FunctionSignature(");
    boolean first = true;

    sb.append("name:");
    if (this.name == null) {
      sb.append("null");
    } else {
      sb.append(this.name);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("inputNames:");
    if (this.inputNames == null) {
      sb.append("null");
    } else {
      sb.append(this.inputNames);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("inputTypes:");
    if (this.inputTypes == null) {
      sb.append("null");
    } else {
      sb.append(this.inputTypes);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("outputNames:");
    if (this.outputNames == null) {
      sb.append("null");
    } else {
      sb.append(this.outputNames);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("outputTypes:");
    if (this.outputTypes == null) {
      sb.append("null");
    } else {
      sb.append(this.outputTypes);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class FunctionSignatureStandardSchemeFactory implements SchemeFactory {
    public FunctionSignatureStandardScheme getScheme() {
      return new FunctionSignatureStandardScheme();
    }
  }

  private static class FunctionSignatureStandardScheme extends StandardScheme<FunctionSignature> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, FunctionSignature struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.name = iprot.readString();
              struct.setNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // INPUT_NAMES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.inputNames = new ArrayList<String>(_list0.size);
                String _elem1;
                for (int _i2 = 0; _i2 < _list0.size; ++_i2)
                {
                  _elem1 = iprot.readString();
                  struct.inputNames.add(_elem1);
                }
                iprot.readListEnd();
              }
              struct.setInputNamesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // INPUT_TYPES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list3 = iprot.readListBegin();
                struct.inputTypes = new ArrayList<String>(_list3.size);
                String _elem4;
                for (int _i5 = 0; _i5 < _list3.size; ++_i5)
                {
                  _elem4 = iprot.readString();
                  struct.inputTypes.add(_elem4);
                }
                iprot.readListEnd();
              }
              struct.setInputTypesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // OUTPUT_NAMES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list6 = iprot.readListBegin();
                struct.outputNames = new ArrayList<String>(_list6.size);
                String _elem7;
                for (int _i8 = 0; _i8 < _list6.size; ++_i8)
                {
                  _elem7 = iprot.readString();
                  struct.outputNames.add(_elem7);
                }
                iprot.readListEnd();
              }
              struct.setOutputNamesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // OUTPUT_TYPES
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list9 = iprot.readListBegin();
                struct.outputTypes = new ArrayList<String>(_list9.size);
                String _elem10;
                for (int _i11 = 0; _i11 < _list9.size; ++_i11)
                {
                  _elem10 = iprot.readString();
                  struct.outputTypes.add(_elem10);
                }
                iprot.readListEnd();
              }
              struct.setOutputTypesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, FunctionSignature struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.name != null) {
        oprot.writeFieldBegin(NAME_FIELD_DESC);
        oprot.writeString(struct.name);
        oprot.writeFieldEnd();
      }
      if (struct.inputNames != null) {
        oprot.writeFieldBegin(INPUT_NAMES_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.inputNames.size()));
          for (String _iter12 : struct.inputNames)
          {
            oprot.writeString(_iter12);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.inputTypes != null) {
        oprot.writeFieldBegin(INPUT_TYPES_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.inputTypes.size()));
          for (String _iter13 : struct.inputTypes)
          {
            oprot.writeString(_iter13);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.outputNames != null) {
        oprot.writeFieldBegin(OUTPUT_NAMES_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.outputNames.size()));
          for (String _iter14 : struct.outputNames)
          {
            oprot.writeString(_iter14);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.outputTypes != null) {
        oprot.writeFieldBegin(OUTPUT_TYPES_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.outputTypes.size()));
          for (String _iter15 : struct.outputTypes)
          {
            oprot.writeString(_iter15);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class FunctionSignatureTupleSchemeFactory implements SchemeFactory {
    public FunctionSignatureTupleScheme getScheme() {
      return new FunctionSignatureTupleScheme();
    }
  }

  private static class FunctionSignatureTupleScheme extends TupleScheme<FunctionSignature> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, FunctionSignature struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetName()) {
        optionals.set(0);
      }
      if (struct.isSetInputNames()) {
        optionals.set(1);
      }
      if (struct.isSetInputTypes()) {
        optionals.set(2);
      }
      if (struct.isSetOutputNames()) {
        optionals.set(3);
      }
      if (struct.isSetOutputTypes()) {
        optionals.set(4);
      }
      oprot.writeBitSet(optionals, 5);
      if (struct.isSetName()) {
        oprot.writeString(struct.name);
      }
      if (struct.isSetInputNames()) {
        {
          oprot.writeI32(struct.inputNames.size());
          for (String _iter16 : struct.inputNames)
          {
            oprot.writeString(_iter16);
          }
        }
      }
      if (struct.isSetInputTypes()) {
        {
          oprot.writeI32(struct.inputTypes.size());
          for (String _iter17 : struct.inputTypes)
          {
            oprot.writeString(_iter17);
          }
        }
      }
      if (struct.isSetOutputNames()) {
        {
          oprot.writeI32(struct.outputNames.size());
          for (String _iter18 : struct.outputNames)
          {
            oprot.writeString(_iter18);
          }
        }
      }
      if (struct.isSetOutputTypes()) {
        {
          oprot.writeI32(struct.outputTypes.size());
          for (String _iter19 : struct.outputTypes)
          {
            oprot.writeString(_iter19);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, FunctionSignature struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(5);
      if (incoming.get(0)) {
        struct.name = iprot.readString();
        struct.setNameIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list20 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.inputNames = new ArrayList<String>(_list20.size);
          String _elem21;
          for (int _i22 = 0; _i22 < _list20.size; ++_i22)
          {
            _elem21 = iprot.readString();
            struct.inputNames.add(_elem21);
          }
        }
        struct.setInputNamesIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TList _list23 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.inputTypes = new ArrayList<String>(_list23.size);
          String _elem24;
          for (int _i25 = 0; _i25 < _list23.size; ++_i25)
          {
            _elem24 = iprot.readString();
            struct.inputTypes.add(_elem24);
          }
        }
        struct.setInputTypesIsSet(true);
      }
      if (incoming.get(3)) {
        {
          org.apache.thrift.protocol.TList _list26 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.outputNames = new ArrayList<String>(_list26.size);
          String _elem27;
          for (int _i28 = 0; _i28 < _list26.size; ++_i28)
          {
            _elem27 = iprot.readString();
            struct.outputNames.add(_elem27);
          }
        }
        struct.setOutputNamesIsSet(true);
      }
      if (incoming.get(4)) {
        {
          org.apache.thrift.protocol.TList _list29 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.outputTypes = new ArrayList<String>(_list29.size);
          String _elem30;
          for (int _i31 = 0; _i31 < _list29.size; ++_i31)
          {
            _elem30 = iprot.readString();
            struct.outputTypes.add(_elem30);
          }
        }
        struct.setOutputTypesIsSet(true);
      }
    }
  }

}

