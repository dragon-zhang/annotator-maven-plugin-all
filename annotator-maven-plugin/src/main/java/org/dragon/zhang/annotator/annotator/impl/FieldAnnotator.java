package org.dragon.zhang.annotator.annotator.impl;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import org.codehaus.plexus.logging.Logger;
import org.dragon.zhang.annotator.annotator.AbstractAnnotator;
import org.dragon.zhang.annotator.annotator.Annotator;
import org.dragon.zhang.annotator.model.JavadocMapping;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangzicheng
 * @date 2021/03/10
 */
public class FieldAnnotator extends AbstractAnnotator {

    public FieldAnnotator(Logger log) {
        super(log);
    }

    @Override
    protected Map<String, Set<JavadocMapping>> needToTag(Set<JavadocMapping> needToTag, JavaClassSource source) {
        Map<String, Set<JavadocMapping>> result = new HashMap<>();
        List<FieldSource<JavaClassSource>> fields = source.getFields();
        for (FieldSource<JavaClassSource> field : fields) {
            result.put(field.getName(), AbstractAnnotator.buildNeedToTag(field, needToTag));
        }
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> initComment(JavaClassSource source) {
        Map<String, Map<String, Object>> comment = new HashMap<>();
        List<FieldSource<JavaClassSource>> fields = source.getFields();
        for (FieldSource<JavaClassSource> field : fields) {
            Map<String, Object> map = new HashMap<>();
            JavaDocSource<FieldSource<JavaClassSource>> fieldDoc = field.getJavaDoc();
            map.put(Annotator.DESCRIPTION_KEY, fieldDoc.getText());
            List<JavaDocTag> fieldTags = fieldDoc.getTags();
            for (JavaDocTag tag : fieldTags) {
                map.put(tag.getName(), tag.getValue());
            }
            comment.put(field.getName(), map);
        }
        return comment;
    }

    @Override
    protected DynamicType.Builder<?> tagAnnotations(DynamicType.Builder<?> builder, Collection<AnnotationDescription> descriptions, String name, int index) {
        return builder.field(ElementMatchers.named(name))
                .annotateField(descriptions);
    }

    @Override
    public ElementType annotateType() {
        return ElementType.FIELD;
    }
}
