package com.xuan.makeo2o;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.CollectionListModel;

import java.util.List;

/**
 * Click MakeO2O Action.
 *
 * @author xuan
 * @since 2019/3/26
 */
public class MakeO2OAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod psiMethod = getPsiMethod(e);
        if (null == psiMethod) {
            return;
        }
        makeO2OMethod(psiMethod);
    }

    private void makeO2OMethod(final PsiMethod psiMethod) {
        new WriteCommandAction.Simple(psiMethod.getProject(), psiMethod.getContainingFile()) {
            @Override
            protected void run() {
                createO2O(psiMethod);
            }
        }.execute();
    }

    /**
     * CreateO2O
     *
     * @param psiMethod PsiMethod
     */
    private void createO2O(PsiMethod psiMethod) {
        String methodName = psiMethod.getName();
        PsiType returnType = psiMethod.getReturnType();
        if (null == returnType) {
            return;
        }
        String returnClassName = returnType.getPresentableText();
        PsiParameter psiParameter = psiMethod.getParameterList().getParameters()[0];
        //ClassName with package
        String parameterClassWithPackage = psiParameter.getType().getInternalCanonicalText();
        //To parse the field, need to load the class of the parameter here
        JavaPsiFacade facade = JavaPsiFacade.getInstance(psiMethod.getProject());
        PsiClass paramentClass = facade.findClass(parameterClassWithPackage, GlobalSearchScope.allScope(psiMethod.getProject()));
        if (null == paramentClass) {
            return;
        }
        List<PsiField> paramentFieldList = new CollectionListModel<>(paramentClass.getFields()).getItems();
        String methodText = buildCode(methodName, returnClassName, psiParameter, paramentFieldList);
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiMethod.getProject());
        PsiMethod toMethod = elementFactory.createMethodFromText(methodText, psiMethod);
        psiMethod.replace(toMethod);
    }

    /**
     * Build code
     *
     * @param methodName        methodName
     * @param returnClassName   returnClassName
     * @param psiParameter      psiParameter
     * @param paramentFieldList paramentFieldList
     * @return code
     */
    private String buildCode(String methodName,
                             String returnClassName,
                             PsiParameter psiParameter,
                             List<PsiField> paramentFieldList) {
        String returnObjName = returnClassName.substring(0, 1).toLowerCase() + returnClassName.substring(1);
        String parameterClass = psiParameter.getText();
        String parameterName = psiParameter.getName();


        //code builder
        StringBuilder builder = new StringBuilder();

        // method name
        builder
                .append("public static ")
                .append(returnClassName)
                .append(" ").append(methodName)
                .append(" (")
                .append(parameterClass)
                .append(" ) {\n");

        //if null code
        builder.append("if ( ")
                .append(parameterName)
                .append("== null ){\n")
                .append("return null;\n}")
                .append(returnClassName)
                .append(" ")
                .append(returnObjName)
                .append("= new ")
                .append(returnClassName)
                .append("();\n");

        for (PsiField field : paramentFieldList) {
            PsiModifierList modifierList = field.getModifierList();
            if (null == modifierList
                    || modifierList.hasModifierProperty(PsiModifier.STATIC)
                    || modifierList.hasModifierProperty(PsiModifier.FINAL)
                    || modifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)
                    || null == field.getName()) {
                continue;
            }
            //set get code
            builder.append(returnObjName)
                    .append(".set")
                    .append(getFirstUpperCase(field.getName()))
                    .append("(")
                    .append(parameterName)
                    .append(".get")
                    .append(getFirstUpperCase(field.getName()))
                    .append("());\n");
        }

        //return code
        builder.append("return ")
                .append(returnObjName)
                .append(";\n");

        builder.append("}\n");
        return builder.toString();
    }

    /**
     * Get string which first letter upper.
     * eq. "abcd" will return "Abcd".
     *
     * @param oldStr orign string
     * @return firstUpperCase string
     */
    private String getFirstUpperCase(String oldStr) {
        return oldStr.substring(0, 1).toUpperCase() + oldStr.substring(1);
    }

    /**
     * Get PsiMethod from AnActionEvent
     *
     * @param e e
     * @return PsiMethod
     */
    private PsiMethod getPsiMethod(AnActionEvent e) {
        PsiElement elementAt = getPsiElement(e);
        if (null == elementAt) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class);
    }

    /**
     * Get PsiElement from AnActionEvent
     *
     * @param e e
     * @return PsiElement
     */
    private PsiElement getPsiElement(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (null == psiFile || null == editor) {
            e.getPresentation().setEnabled(false);
            return null;
        }

        //PsiElement used to get the current cursor
        int offset = editor.getCaretModel().getOffset();
        return psiFile.findElementAt(offset);
    }

}
