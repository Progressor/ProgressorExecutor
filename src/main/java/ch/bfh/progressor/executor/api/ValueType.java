package ch.bfh.progressor.executor.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ch.bfh.progressor.executor.thrift.executorConstants;

/**
 * Represents a generic value type.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public interface ValueType {

	/**
	 * Gets the base (non-generic) type.
	 *
	 * @return base (non-generic) type
	 */
	ValueType.BaseType getBaseType();

	/**
	 * Gets the type's generic parameters.
	 *
	 * @return type's generic parameters
	 */
	List<ValueType> getGenericParameters();

	/**
	 * Represents the supported base types.
	 *
	 * @author strut1, touwm1 &amp; weidj1
	 */
	enum BaseType {

		/**
		 * {@link String}
		 */
		STRING(executorConstants.TypeString, 0),

		/**
		 * {@link Character}
		 */
		CHARACTER(executorConstants.TypeCharacter, 0),

		/**
		 * {@link Boolean}
		 */
		BOOLEAN(executorConstants.TypeBoolean, 0),

		/**
		 * 8-bit signed integer ({@link Byte})
		 */
		INT8(executorConstants.TypeInt8, 0),

		/**
		 * 16-bit signed integer ({@link Short})
		 */
		INT16(executorConstants.TypeInt16, 0),

		/**
		 * 32-bit signed integer ({@link Integer})
		 */
		INT32(executorConstants.TypeInt32, 0),

		/**
		 * 64-bit signed integer ({@link Long})
		 */
		INT64(executorConstants.TypeInt64, 0),

		/**
		 * 32-bit floating-point number ({@link Float})
		 */
		FLOAT32(executorConstants.TypeFloat32, 0),

		/**
		 * 32-bit floating-point number ({@link Double})
		 */
		FLOAT64(executorConstants.TypeFloat64, 0),

		/**
		 * decimal (if not available: floating-point) number with highest available precision ({@link BigDecimal}
		 */
		DECIMAL(executorConstants.TypeDecimal, 0),

		/**
		 * C-like array
		 */
		ARRAY(executorConstants.TypeContainerArray, 1),

		/**
		 * dynamically growing ordered {@link List}
		 */
		LIST(executorConstants.TypeContainerList, 1),

		/**
		 * dynamically growing unordered, distinct {@link Set}
		 */
		SET(executorConstants.TypeContainerSet, 1),

		/**
		 * dynamically growing {@link Map} of unique key-value pairs
		 */
		MAP(executorConstants.TypeContainerMap, 2);

		private final String name;
		private final int dimensions;

		BaseType(String name, int dimensions) {
			this.name = name;
			this.dimensions = dimensions;
		}

		/**
		 * Gets the name of the type.
		 *
		 * @return name of the type
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Gets the number of dimensions of the type.
		 *
		 * @return number of dimensions of the type
		 */
		public int getDimensions() {
			return this.dimensions;
		}

		/**
		 * Gets the name of the type.
		 *
		 * @return name of the type
		 */
		@Override
		public String toString() {
			return this.name;
		}
	}
}
