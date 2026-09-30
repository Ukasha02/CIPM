package tools.cipm.seff.testutil;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

/**
 * Registers the ".java" extension with a plain {@link ResourceFactoryImpl} in the shared,
 * JVM-wide {@link Resource.Factory.Registry}, for tests that build a real .java-backed
 * {@code Resource}/{@code CompilationUnit} outside a full IDE launch. In a full IDE launch,
 * some bundle's Activator registers this factory automatically; in a headless test run
 * (useUIHarness=false) nothing does, so {@code ResourceSet#createResource(...)} for a .java
 * URI would otherwise silently return null. The registry is a plain shared map, so registering
 * twice is harmless - call once per test class, typically from a {@code @BeforeAll}, to keep
 * intent clear at the call site.
 */
public final class JavaResourceRegistration {

	private JavaResourceRegistration() {
	}

	/** Registers the ".java" extension-to-factory mapping described above. */
	public static void ensureJavaExtensionRegistered() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("java", new ResourceFactoryImpl());
	}
}
