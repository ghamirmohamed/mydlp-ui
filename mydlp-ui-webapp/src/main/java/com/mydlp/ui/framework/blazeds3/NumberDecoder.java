package com.mydlp.ui.framework.blazeds3;

import java.math.BigDecimal;
import java.math.BigInteger;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.translator.decoder.ActionScriptDecoder;
import flex.messaging.io.amf.translator.decoder.DecoderFactory;

/**
 * Decode an ActionScript type (a string or a double) to a Java number (of any
 * type).
 * 
 * Imported from http://flexblog.faratasystems.com/2010/01/07/how-to-keep-numeric-null-value-from-turning-into-zeros-in-blazeds
 */
public class NumberDecoder extends ActionScriptDecoder {
	@SuppressWarnings("rawtypes")
	public Object decodeObject(Object shell, Object encodedObject,
			Class desiredClass) {
		Object result = null;

		if (encodedObject != null && encodedObject instanceof String) {
			String str = ((String) encodedObject).trim();
			try {
				// Short-ciruit String -> BigInteger,BigDecimal to avoid loss
				// of precision that occurs if we go through Double
				if (!SerializationContext.getSerializationContext().legacyBigNumbers) {
					if (BigInteger.class.equals(desiredClass)) {
						result = new BigInteger(str);
						return result;
					} else if (BigDecimal.class.equals(desiredClass)) {
						result = new BigDecimal(str);
						return result;
					}
				}

				Double dbl = new Double(str);
				encodedObject = dbl;
			} catch (NumberFormatException nfe) {
				DecoderFactory.invalidType(encodedObject, desiredClass);
			}
		}

		if (encodedObject instanceof Number || encodedObject == null) {
			Double dbl;
			if (desiredClass.isPrimitive()) {
				if (encodedObject == null)
					dbl = new Double(0);
				else
					dbl = new Double(((Number) encodedObject).doubleValue());

				if (dbl.equals(Double.NaN))
					result = null;

				else if (Object.class.equals(desiredClass)
						|| Double.TYPE.equals(desiredClass))
					result = dbl;
				else if (Integer.TYPE.equals(desiredClass))
					result = new Integer(dbl.intValue());
				else if (Long.TYPE.equals(desiredClass))
					result = new Long(dbl.longValue());
				else if (Float.TYPE.equals(desiredClass))
					result = new Float(dbl.floatValue());
				else if (Short.TYPE.equals(desiredClass))
					result = new Short(dbl.shortValue());
				else if (Byte.TYPE.equals(desiredClass))
					result = new Byte(dbl.byteValue());
			} else if (encodedObject != null) {
				dbl = new Double(((Number) encodedObject).doubleValue());
				if (dbl.equals(Double.NaN))
					result = null;

				else if (Object.class.equals(desiredClass)
						|| Number.class.equals(desiredClass)
						|| Double.class.equals(desiredClass))
					result = dbl;
				else if (Integer.class.equals(desiredClass))
					result = new Integer(dbl.intValue());
				else if (Long.class.equals(desiredClass))
					result = new Long(dbl.longValue());
				else if (Float.class.equals(desiredClass))
					result = new Float(dbl.floatValue());
				else if (Short.class.equals(desiredClass))
					result = new Short(dbl.shortValue());
				else if (BigDecimal.class.equals(desiredClass)) {
					// Though this is a little slower than using the
					// double constructor for BigDecimal, it yields a rounded
					// result which is more predicable (see the Javadoc for
					// BigDecimal and bug: 182378).
					if (SerializationContext.getSerializationContext().legacyBigNumbers)
						result = new BigDecimal(dbl.doubleValue());
					else
						result = new BigDecimal(String.valueOf(dbl));
				} else if (Byte.class.equals(desiredClass))
					result = new Byte(dbl.byteValue());
				else if (BigInteger.class.equals(desiredClass)) {
					// Must have no special characters or whitespace
					String val = null;
					long l = dbl.longValue();
					if (l > Integer.MAX_VALUE) {
						Long lo = new Long(dbl.longValue());
						val = lo.toString().toUpperCase();
						int suffix = val.indexOf("L");
						if (suffix != -1)
							val = val.substring(0, suffix);
					} else {
						Integer i = new Integer(dbl.intValue());
						val = i.toString();
					}
					result = new BigInteger(val.trim());
				}
			} else {
				result = null;
			}
		} else {
			DecoderFactory.invalidType(encodedObject, desiredClass);
		}

		return result;
	}
}
