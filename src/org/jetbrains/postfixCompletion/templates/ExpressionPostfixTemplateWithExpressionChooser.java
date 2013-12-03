package org.jetbrains.postfixCompletion.templates;

import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.util.PsiExpressionTrimRenderer;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.postfixCompletion.util.CommonUtils;

import java.util.List;

/**
 * @author ignatov
 */
public abstract class ExpressionPostfixTemplateWithExpressionChooser extends PostfixTemplate {
  public ExpressionPostfixTemplateWithExpressionChooser(@Nullable String name, @Nullable String description, @Nullable String example) {
    super(name, description, example);
  }

  @Override
  public boolean isApplicable(@NotNull PsiElement context, @NotNull Document copyDocument, int newOffset) {
    Editor editor = EditorFactory.getInstance().createEditor(copyDocument);
    boolean result;
    try {
      result = !getExpressions(context, editor, newOffset).isEmpty();
    }
    finally {
      EditorFactory.getInstance().releaseEditor(editor);
    }
    return result;
  }

  @Override
  public void expand(@NotNull PsiElement context, @NotNull final Editor editor) {
    List<PsiExpression> expressions = getExpressions(context, editor, editor.getCaretModel().getOffset());

    if (expressions.isEmpty()) {
      CommonUtils.showErrorHint(context.getProject(), editor);
    }
    else if (expressions.size() == 1) {
      doIt(editor, expressions.get(0));
    }
    else {
      IntroduceTargetChooser.showChooser(
        editor, expressions,
        new Pass<PsiExpression>() {
          public void pass(@NotNull final PsiExpression e) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                CommandProcessor.getInstance().executeCommand(e.getProject(), new Runnable() {
                  public void run() {
                    doIt(editor, e);
                  }
                }, "Expand postfix template", PostfixLiveTemplate.POSTFIX_TEMPLATE_ID);
              }
            });
          }
        },
        new PsiExpressionTrimRenderer.RenderFunction(),
        "Expressions", 0, ScopeHighlighter.NATURAL_RANGER);
    }
  }

  @NotNull
  public static List<PsiExpression> getExpressions(@NotNull PsiElement context, @NotNull Editor editor, int offset) {
    return IntroduceVariableBase.collectExpressions(context.getContainingFile(), editor, offset);
  }

  protected abstract void doIt(@NotNull Editor editor, @NotNull PsiExpression expression);
}