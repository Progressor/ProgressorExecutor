package ch.bfh.progressor.executor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.bfh.progressor.executor.api.ExecutorException;
import ch.bfh.progressor.executor.api.FunctionSignature;
import ch.bfh.progressor.executor.api.ValueType;

/**
 * Read-only implementation of a {@link FunctionSignature}.
 *
 * @author strut1, touwm1 &amp; weidj1
 */
public class FunctionSignatureImpl implements FunctionSignature {

	private final String name;
	private final List<String> inputNames, outputNames;
	private final List<ValueType> inputTypes, outputTypes;

	/**
	 * Construct a new {@link FunctionSignature}.
	 *
	 * @param name        name of the function
	 * @param inputNames  names of the function's input parameters
	 * @param inputTypes  types of the function's input parameters
	 * @param outputNames names of the function's output parameters
	 * @param outputTypes types of the function's output parameters
	 */
	public FunctionSignatureImpl(String name, List<String> inputNames, List<ValueType> inputTypes, List<String> outputNames, List<ValueType> outputTypes) {

		this.name = name;
		this.inputNames = Collections.unmodifiableList(inputNames);
		this.inputTypes = Collections.unmodifiableList(inputTypes);
		this.outputNames = Collections.unmodifiableList(outputNames);
		this.outputTypes = Collections.unmodifiableList(outputTypes);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<String> getInputNames() {
		return this.inputNames; //is unmodifiable
	}

	@Override
	public List<ValueType> getInputTypes() {
		return this.inputTypes; //is unmodifiable
	}

	@Override
	public List<String> getOutputNames() {
		return this.outputNames; //is unmodifiable
	}

	@Override
	public List<ValueType> getOutputTypes() {
		return this.outputTypes; //is unmodifiable
	}

	/**
	 * Converts thrift {@link ch.bfh.progressor.executor.thrift.FunctionSignature}s to custom {@link FunctionSignature} instances.
	 *
	 * @param functions thrift functions to convert
	 *
	 * @return custom {@link FunctionSignature} instances
	 *
	 * @throws ExecutorException if conversation failed
	 */
	public static List<FunctionSignature> convertFromThrift(List<ch.bfh.progressor.executor.thrift.FunctionSignature> functions) throws ExecutorException {

		List<FunctionSignature> result = new ArrayList<>(functions.size());
		for (ch.bfh.progressor.executor.thrift.FunctionSignature function : functions) {
			List<ValueType> inputTypes = new ArrayList<>(function.getInputTypesSize()), outputTypes = new ArrayList<>(function.getOutputTypesSize());
			for (String inputType : function.getInputTypes())
				inputTypes.add(ValueTypeImpl.parse(inputType));
			for (String outputType : function.getOutputTypes())
				outputTypes.add(ValueTypeImpl.parse(outputType));
			result.add(new FunctionSignatureImpl(function.getName(), function.getInputNames(), inputTypes, function.getOutputNames(), outputTypes));
		}

		return result;
	}
}
