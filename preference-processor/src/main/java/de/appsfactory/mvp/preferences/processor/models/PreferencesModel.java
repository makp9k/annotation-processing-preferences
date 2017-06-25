package de.appsfactory.mvp.preferences.processor.models;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * Created by Admin on 24.06.2017.
 */

public class PreferencesModel {

    private static final String BASE_PACKAGE_NAME = "de.appsfactory.mvp.preferences";

    private final String packageName;
    private final String originClassName;
    private final List<PreferenceEntryModel> entries = new LinkedList<>();

    public PreferencesModel(String packageName, String originClassName) {
        this.packageName = packageName;
        this.originClassName = originClassName;
    }

    public List<PreferenceEntryModel> getEntries() {
        return entries;
    }

    public JavaFile brewJava() {
        return JavaFile.builder(packageName, createType())
                .build();
    }

    private TypeSpec createType() {
        TypeSpec.Builder result = TypeSpec
                .classBuilder(originClassName + "Impl")
                .addSuperinterface(ClassName.bestGuess(originClassName))
                .addModifiers(Modifier.PUBLIC);

        List<FieldSpec> fields = new LinkedList<>();
        for (PreferenceEntryModel entry : entries) {
            FieldSpec field = createField(entry);
            fields.add(field);
            result.addField(field);
        }

        for (FieldSpec field : fields) {
            result.addMethod(createGetter(field));
        }

        result.addMethod(createConstructor(fields));

        return result.build();
    }

    private FieldSpec createField(PreferenceEntryModel entry) {
        return FieldSpec
                .builder(createTypeName(entry), entry.getName(), Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private TypeName createTypeName(PreferenceEntryModel entry) {
        return ParameterizedTypeName.get(
                ClassName.get(BASE_PACKAGE_NAME, "Preference"),
                ClassName.bestGuess(entry.getType())
        );
    }

    private MethodSpec createConstructor(List<FieldSpec> fields) {
        ParameterSpec contextParameter = ParameterSpec.builder(
                ClassName.get("android.content", "Context"),
                "context"
        )
                .build();

        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement(
                        "final $T sharedPreferences = context.getSharedPreferences($S, Context.MODE_PRIVATE)",
                        ClassName.get("android.content", "SharedPreferences"),
                        packageName.isEmpty() ? originClassName : packageName + "." + originClassName
                )
                .addParameter(contextParameter);

        for (PreferenceEntryModel entry : entries) {
            if (entry.getCustomAdapter() != null) {
                addCustomAdapterInitializer(builder, entry.getCustomAdapter());
            }
        }

        for (PreferenceEntryModel entry : entries) {
            addFieldInitializer(builder, entry);
        }

        return builder.build();
    }

    private void addCustomAdapterInitializer(MethodSpec.Builder constructorBuilder, String adapterClassName) {
        ClassName className = ClassName.bestGuess(adapterClassName);
        constructorBuilder.addStatement(
                "$T $L = new $T()",
                className,
                "adapter" + className.simpleName(),
                className
        );
        constructorBuilder.addStatement(
                "$L.init(context)",
                "adapter" + className.simpleName()
        );
    }

    private void addFieldInitializer(MethodSpec.Builder constructorBuilder, PreferenceEntryModel entry) {
        if (entry.getCustomAdapter() == null) {
            constructorBuilder.addStatement(
                    "$L = new $T(sharedPreferences, $S, $L)",
                    entry.getName(),
                    ClassName.get(BASE_PACKAGE_NAME + ".concrete", entry.getPreferenceTypeName()),
                    entry.getName(),
                    entry.getDefaultValue()
            );
        } else {
            constructorBuilder.addStatement(
                    "$L = new $T(sharedPreferences, $S, $L, $L)",
                    entry.getName(),
                    ClassName.get(BASE_PACKAGE_NAME + ".concrete", entry.getPreferenceTypeName()),
                    entry.getName(),
                    entry.getDefaultValue(),
                    "adapter" + ClassName.bestGuess(entry.getCustomAdapter()).simpleName()
            );
        }
    }

    private MethodSpec createGetter(FieldSpec fieldSpec) {
        return MethodSpec
                .methodBuilder(fieldSpec.name)
                .returns(fieldSpec.type)
                .addStatement("return $N", fieldSpec)
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    @Override
    public String toString() {
        return "PreferencesModel{" +
                "packageName='" + packageName + '\'' +
                ", originClassName='" + originClassName + '\'' +
                ", entries=" + entries +
                '}';
    }
}
