package org.globaltester.lib.xstream;

import static org.globaltester.lib.xstream.ShouldSerializeMemberInstruction.SERIALIZE;
import static org.globaltester.lib.xstream.ShouldSerializeMemberInstruction.DO_NOT_SERIALIZE;

import java.lang.reflect.Constructor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.basic.BigDecimalConverter;
import com.thoughtworks.xstream.converters.basic.BigIntegerConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.converters.basic.ByteConverter;
import com.thoughtworks.xstream.converters.basic.CharConverter;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.basic.DoubleConverter;
import com.thoughtworks.xstream.converters.basic.FloatConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.converters.basic.LongConverter;
import com.thoughtworks.xstream.converters.basic.NullConverter;
import com.thoughtworks.xstream.converters.basic.ShortConverter;
import com.thoughtworks.xstream.converters.basic.StringBufferConverter;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.converters.basic.URIConverter;
import com.thoughtworks.xstream.converters.basic.URLConverter;
import com.thoughtworks.xstream.converters.collections.ArrayConverter;
import com.thoughtworks.xstream.converters.collections.BitSetConverter;
import com.thoughtworks.xstream.converters.collections.CharArrayConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.collections.PropertiesConverter;
import com.thoughtworks.xstream.converters.collections.SingletonCollectionConverter;
import com.thoughtworks.xstream.converters.collections.SingletonMapConverter;
import com.thoughtworks.xstream.converters.collections.TreeMapConverter;
import com.thoughtworks.xstream.converters.collections.TreeSetConverter;
import com.thoughtworks.xstream.converters.extended.ColorConverter;
import com.thoughtworks.xstream.converters.extended.DynamicProxyConverter;
import com.thoughtworks.xstream.converters.extended.FileConverter;
import com.thoughtworks.xstream.converters.extended.FontConverter;
import com.thoughtworks.xstream.converters.extended.GregorianCalendarConverter;
import com.thoughtworks.xstream.converters.extended.JavaClassConverter;
import com.thoughtworks.xstream.converters.extended.JavaFieldConverter;
import com.thoughtworks.xstream.converters.extended.JavaMethodConverter;
import com.thoughtworks.xstream.converters.extended.LocaleConverter;
import com.thoughtworks.xstream.converters.extended.LookAndFeelConverter;
import com.thoughtworks.xstream.converters.extended.SqlDateConverter;
import com.thoughtworks.xstream.converters.extended.SqlTimeConverter;
import com.thoughtworks.xstream.converters.extended.SqlTimestampConverter;
import com.thoughtworks.xstream.converters.extended.TextAttributeConverter;
import com.thoughtworks.xstream.converters.reflection.ExternalizableConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.core.util.SelfStreamingInstanceChecker;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;



public class XstreamFactory {
	
	public static XStream get(HierarchicalStreamDriver hsd,
			ShouldSerializeMemberInstruction instruction,
			ClassLoader... classLoaders) {
		
		XStream xstream = new XStream (hsd)
		{
			@Override
			protected MapperWrapper wrapMapper (MapperWrapper next) 
			{
				return new MapperWrapper(next) {
					
					@SuppressWarnings("rawtypes")
					public boolean shouldSerializeMember(Class definedIn, String fieldName) {
						
						if(instruction != null) {
							byte result = instruction.shouldSerializeMember(definedIn, fieldName);
							
							switch (result) {
							case SERIALIZE:
								return true;
							case DO_NOT_SERIALIZE:
								return false;
							}
						}
						
						return super.shouldSerializeMember (definedIn, fieldName);
					}
				};
			}
			
			@Override
			@SuppressWarnings(value = { "deprecation" }) 
			protected void setupConverters() {
				Mapper mapper = getMapper();
				ReflectionProvider reflectionProvider = getReflectionProvider();
				ClassLoaderReference classLoaderReference = getClassLoaderReference();

				registerConverter(new ReflectionConverter(mapper, reflectionProvider), PRIORITY_VERY_LOW);

				registerConverter(new SerializableConverter(mapper, reflectionProvider, classLoaderReference),
						PRIORITY_LOW);
				registerConverter(new ExternalizableConverter(mapper, classLoaderReference), PRIORITY_LOW);

				registerConverter(new NullConverter(), PRIORITY_VERY_HIGH);
				registerConverter(new IntConverter(), PRIORITY_NORMAL);
				registerConverter(new FloatConverter(), PRIORITY_NORMAL);
				registerConverter(new DoubleConverter(), PRIORITY_NORMAL);
				registerConverter(new LongConverter(), PRIORITY_NORMAL);
				registerConverter(new ShortConverter(), PRIORITY_NORMAL);
				registerConverter((Converter) new CharConverter(), PRIORITY_NORMAL);
				registerConverter(new BooleanConverter(), PRIORITY_NORMAL);
				registerConverter(new ByteConverter(), PRIORITY_NORMAL);

				registerConverter(new StringConverter(), PRIORITY_NORMAL);
				registerConverter(new StringBufferConverter(), PRIORITY_NORMAL);
				registerConverter(new DateConverter(), PRIORITY_NORMAL);
				registerConverter(new BitSetConverter(), PRIORITY_NORMAL);
				registerConverter(new URIConverter(), PRIORITY_NORMAL);
				registerConverter(new URLConverter(), PRIORITY_NORMAL);
				registerConverter(new BigIntegerConverter(), PRIORITY_NORMAL);
				registerConverter(new BigDecimalConverter(), PRIORITY_NORMAL);

				registerConverter(new ArrayConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new CharArrayConverter(), PRIORITY_NORMAL);
				registerConverter(new CollectionConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new MapConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new TreeMapConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new TreeSetConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new SingletonCollectionConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new SingletonMapConverter(mapper), PRIORITY_NORMAL);
				registerConverter(new PropertiesConverter(), PRIORITY_NORMAL);

				registerConverter(new FileConverter(), PRIORITY_NORMAL);
				if (JVM.isSQLAvailable()) {
					registerConverter(new SqlTimestampConverter(), PRIORITY_NORMAL);
					registerConverter(new SqlTimeConverter(), PRIORITY_NORMAL);
					registerConverter(new SqlDateConverter(), PRIORITY_NORMAL);
				}
				registerConverter(new DynamicProxyConverter(mapper, classLoaderReference), PRIORITY_NORMAL);
				registerConverter(new JavaClassConverter(classLoaderReference), PRIORITY_NORMAL);
				registerConverter(new JavaMethodConverter(classLoaderReference), PRIORITY_NORMAL);
				registerConverter(new JavaFieldConverter(classLoaderReference), PRIORITY_NORMAL);
				if (JVM.isAWTAvailable()) {
					registerConverter(new FontConverter(mapper), PRIORITY_NORMAL);
					registerConverter(new ColorConverter(), PRIORITY_NORMAL);
					registerConverter(new TextAttributeConverter(), PRIORITY_NORMAL);
				}
				if (JVM.isSwingAvailable()) {
					registerConverter(new LookAndFeelConverter(mapper, reflectionProvider), PRIORITY_NORMAL);
				}
				registerConverter(new LocaleConverter(), PRIORITY_NORMAL);
				registerConverter(new GregorianCalendarConverter(), PRIORITY_NORMAL);

				ConverterLookup converterLookup = getConverterLookup();

				if (JVM.is14()) {
					// late bound converters - allows XStream to be compiled on
					// earlier JDKs
					registerConverterDynamically("com.thoughtworks.xstream.converters.extended.SubjectConverter",
							PRIORITY_NORMAL, new Class[] { Mapper.class }, new Object[] { mapper });

					registerConverterDynamically("com.thoughtworks.xstream.converters.extended.ThrowableConverter",
							PRIORITY_NORMAL, new Class[] { ConverterLookup.class }, new Object[] { converterLookup });
					registerConverterDynamically(
							"com.thoughtworks.xstream.converters.extended.StackTraceElementConverter", PRIORITY_NORMAL,
							null, null);
					registerConverterDynamically("com.thoughtworks.xstream.converters.extended.CurrencyConverter",
							PRIORITY_NORMAL, null, null);
					registerConverterDynamically("com.thoughtworks.xstream.converters.extended.RegexPatternConverter",
							PRIORITY_NORMAL, null, null);
					registerConverterDynamically("com.thoughtworks.xstream.converters.extended.CharsetConverter",
							PRIORITY_NORMAL, null, null);
				}

				if (JVM.is15()) {
					// late bound converters - allows XStream to be compiled on
					// earlier JDKs
					// if (JVM.loadClassForName("javax.xml.datatype.Duration")
					// != null) {
					// registerConverterDynamically(
					// "com.thoughtworks.xstream.converters.extended.DurationConverter",
					// PRIORITY_NORMAL, null, null);
					// }
					registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumConverter",
							PRIORITY_NORMAL, null, null);
					registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumSetConverter",
							PRIORITY_NORMAL, new Class[] { Mapper.class }, new Object[] { mapper });
					registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumMapConverter",
							PRIORITY_NORMAL, new Class[] { Mapper.class }, new Object[] { mapper });
					registerConverterDynamically("com.thoughtworks.xstream.converters.basic.StringBuilderConverter",
							PRIORITY_NORMAL, null, null);
					registerConverterDynamically("com.thoughtworks.xstream.converters.basic.UUIDConverter",
							PRIORITY_NORMAL, null, null);
				}

				registerConverter(new SelfStreamingInstanceChecker(converterLookup, this), PRIORITY_NORMAL);
			}

			private void registerConverterDynamically(String className, int priority, Class<?>[] constructorParamTypes,
					Object[] constructorParamValues) {

				ClassLoaderReference classLoaderReference = getClassLoaderReference();

				try {
					Class<?> type = Class.forName(className, false, classLoaderReference.getReference());
					Constructor<?> constructor = type.getConstructor(constructorParamTypes);
					Object instance = constructor.newInstance(constructorParamValues);
					if (instance instanceof Converter) {
						registerConverter((Converter) instance, priority);
					} else if (instance instanceof SingleValueConverter) {
						registerConverter((SingleValueConverter) instance, priority);
					}
				} catch (Exception e) {
					throw new com.thoughtworks.xstream.InitializationException(
							"Could not instantiate converter : " + className, e);
				}
			}
		};
		
		for (ClassLoader loader : classLoaders){
			((CompositeClassLoader) xstream.getClassLoader()).add(loader);	
		}
		
		return xstream;
	}
	
	public static XStream get(HierarchicalStreamDriver hsd,
			ClassLoader... classLoaders) {
		
		return get(hsd, null, classLoaders);
	}
	
}
