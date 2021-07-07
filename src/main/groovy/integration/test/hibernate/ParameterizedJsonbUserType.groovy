package integration.test.hibernate

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.ParameterizedType
import org.hibernate.usertype.UserType
import org.postgresql.util.PGobject

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * UserType customizado para Hibernate, para serializar como o tipo Jsonb para
 * Postgres baseado em parametros passados no mapeamento.
 *
 * Aceita os parametros:
 * `typeReference` ({@link ParameterizedJsonbUserType#TYPE_REFERENCE_KEY})
 * do tipo {@link com.fasterxml.jackson.core.type.TypeReference}
 *
 * e `type` ({@link ParameterizedJsonbUserType#TYPE_KEY})
 * do tipo {@link java.lang.reflect.Type}.
 *
 * Obs.: A classe dá pra preferência para o `typeReference` quando os dois
 * parâmetros são definidos
 */
@CompileStatic
class ParameterizedJsonbUserType<T> implements UserType, ParameterizedType {

	private static final int SQL_TYPE = 90022

	public static final String TYPE_REFERENCE_KEY = 'typeReference'
	public static final String TYPE_KEY = 'type'

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(MapperFeature.USE_ANNOTATIONS, false)

	Optional<TypeReference<T>> typeReference

	Optional<Class<T>> clazz

	@Override
	int[] sqlTypes() {
		return SQL_TYPE as int[]
	}

	@Override
	Class returnedClass() {
		return typeReference.map { it.type as Class }.orElseGet(clazz.&get)
	}

	@Override
	void setParameterValues(Properties parameters) {
		this.typeReference = getTypeReferenceOfParams(parameters)
		this.clazz = (Optional) getClassOfParams(parameters)

		if (!typeReference.isPresent() && !clazz.isPresent()) {
			throw new RuntimeException("Classe alvo do UserType para Postgres Jsonb não foi definida")
		}
	}

	@Override
	@SuppressWarnings("EqualsOverloaded")
	boolean equals(Object x, Object y) throws HibernateException {
		return Objects.equals(x, y)
	}

	@Override
	int hashCode(Object x) throws HibernateException {
		return Objects.hashCode(x)
	}

	@Override
	Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		PGobject o = rs.getObject(names[0]) as PGobject
		String jsonString = o?.value

		return jsonString == null ? null : readFromString(jsonString)
	}

	@Override
	void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if (value == null) {
			st.setNull(index, Types.OTHER)
		} else {
			st.setObject(index, MAPPER.writeValueAsString(value), Types.OTHER)
		}
	}

	@Override
	Object deepCopy(Object value) throws HibernateException {
		return value == null ? null : readFromString(MAPPER.writeValueAsString(value))
	}

	@Override
	boolean isMutable() {
		return true
	}

	@Override
	Serializable disassemble(Object value) throws HibernateException {
		return MAPPER.writeValueAsString(value)
	}

	@Override
	final Object assemble(Serializable cached, Object owner) throws HibernateException {
		return readFromString(cached as String)
	}

	@Override
	Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original
	}

	private Object readFromString(final String txt) {
		if (typeReference.present) {
			return MAPPER.readValue(txt, typeReference.get())
		}

		if (clazz.present) {
			return MAPPER.readValue(txt, clazz.get())
		}

		throw new RuntimeException('UserType mal configurado, não há classe alvo definida')
	}

	private static <TR> Optional<TypeReference<TR>> getTypeReferenceOfParams(final Properties params) {
		return Optional.ofNullable(params[TYPE_REFERENCE_KEY])
				.filter { it instanceof TypeReference }
				.map { (TypeReference) it }
	}

	private static Optional<Class> getClassOfParams(final Properties params) {
		return Optional.ofNullable(params[TYPE_KEY])
				.filter { it instanceof Class }
				.map { (Class) it }
	}

}
