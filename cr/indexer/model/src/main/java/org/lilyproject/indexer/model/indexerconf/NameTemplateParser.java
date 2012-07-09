package org.lilyproject.indexer.model.indexerconf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lilyproject.repository.api.QName;
import org.lilyproject.repository.api.Repository;
import org.lilyproject.util.repo.SystemFields;
import org.w3c.dom.Element;

public class NameTemplateParser {

    private static Pattern varPattern = Pattern.compile("\\$\\{([^\\}]+)\\}");
    private static Pattern exprPattern = Pattern.compile("([^\\?]+)\\?([^:]+)(?::(.+))?");
    private static Pattern variantPropertyPattern = Pattern.compile("@vprop:([^:]+)");
    private static Pattern fieldPattern = Pattern.compile("([^:]+):([^:])?");

    // used for parsing qnames
    private Repository repository;
    private SystemFields systemFields;

    public NameTemplateParser() {
        this(null, null);
    }

    public NameTemplateParser(Repository repository, SystemFields systemFields) {
        this.repository = repository;
        this.systemFields = systemFields;
    }

    public NameTemplate parse(String template) throws IndexerConfException, NameTemplateException {
        return parse(template, null);
    }

    public NameTemplate parse(String template, NameTemplateValidator validator) throws IndexerConfException, NameTemplateException {
        return parse(null, template, validator);
    }

    // FIXME: Not very clean that this can throw IndexerConfException
    public NameTemplate parse(Element el, String template, NameTemplateValidator validator) throws IndexerConfException, NameTemplateException {
        List<TemplatePart> parts = new ArrayList<TemplatePart>();
        int pos = 0;
        Matcher matcher = varPattern.matcher(template);
        while (pos < template.length()) {
            if (matcher.find(pos)) {
                int start = matcher.start();
                if (start > pos) {
                    parts.add(new LiteralTemplatePart(template.substring(pos, start)));
                }

                String expr = matcher.group(1);
                Matcher exprMatcher = exprPattern.matcher(expr);
                Matcher atVariantPropMatcher = variantPropertyPattern.matcher(expr);
                Matcher fieldMatcher = fieldPattern.matcher(expr);
                if (exprMatcher.matches()) {
                    String condition = exprMatcher.group(1);
                    String trueValue = exprMatcher.group(2);
                    String falseValue = exprMatcher.group(3) != null ? exprMatcher.group(3) : "";

                    parts.add(buildConditionTemplatePart(el, template, condition, trueValue, falseValue, validator));
                } else if (atVariantPropMatcher.matches()){
                    parts.add(new VariantPropertyTemplatePart(atVariantPropMatcher.group(1)));
                } else if (fieldMatcher.matches()){
                    parts.add(buildFieldTemplatePart(el, template, expr));
                } else {
                    if (validator != null) {
                        validator.validateVariable(expr);
                    }
                    parts.add(new VariableTemplatePart(expr));
                }

                pos = matcher.end();
            } else {
                parts.add(new LiteralTemplatePart(template.substring(pos)));
                break;
            }
        }

        return new NameTemplate(template, parts);
    }

    private TemplatePart buildFieldTemplatePart(Element el, String template, String expr) throws IndexerConfException {
        QName field = ConfUtil.parseQName(expr, el);
        return new FieldTemplatePart(field);
    }

    private TemplatePart buildConditionTemplatePart(Element el, String template, String condition, String trueValue,
            String falseValue, NameTemplateValidator validator) throws NameTemplateException {
        if (validator != null) {
            validator.validateCondition(condition, trueValue, falseValue);
        }
        return new ConditionalTemplatePart(condition, trueValue, falseValue);
    }

}