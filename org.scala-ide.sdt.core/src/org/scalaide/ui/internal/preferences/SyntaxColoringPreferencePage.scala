package org.scalaide.ui.internal.preferences

import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.BOOLEAN
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.scalaide.core.IScalaPlugin
import org.scalaide.ui.syntax.ScalaSyntaxClasses
import org.scalaide.ui.syntax.ScalaSyntaxClasses._
import org.scalaide.ui.syntax.preferences.BaseSyntaxColoringPreferencePage
import org.scalaide.util.eclipse.SWTUtils.noArgFnToSelectionAdapter
import org.scalaide.util.ui.SWTUtils.gridData

/** Syntax Coloring preference page for the Scala editors.
 */
class SyntaxColoringPreferencePage extends BaseSyntaxColoringPreferencePage(
  ScalaSyntaxClasses.categories,
  scalaSyntacticCategory,
  IScalaPlugin().getPreferenceStore,
  SyntaxColoringPreferencePage.PreviewText,
  ScalaPreviewerFactoryConfiguration) {

  import org.scalaide.util.ui.SWTUtils._

  private var enableSemanticHighlightingCheckBox: Button = _
  private var extraAccuracyCheckBox: Button = _
  private var strikethroughDeprecatedCheckBox: Button = _

  override def additionalOverlayKeys: List[OverlayKey] = List(
    new OverlayKey(BOOLEAN, ENABLE_SEMANTIC_HIGHLIGHTING),
    new OverlayKey(BOOLEAN, USE_SYNTACTIC_HINTS),
    new OverlayKey(BOOLEAN, STRIKETHROUGH_DEPRECATED))

  override def additionalPerformDefaults() {
    enableSemanticHighlightingCheckBox.setSelection(overlayStore getBoolean ENABLE_SEMANTIC_HIGHLIGHTING)
    extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
    strikethroughDeprecatedCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
    extraAccuracyCheckBox.setSelection(overlayStore getBoolean USE_SYNTACTIC_HINTS)
    strikethroughDeprecatedCheckBox.setSelection(overlayStore getBoolean STRIKETHROUGH_DEPRECATED)
  }

  override def additionalCreateContent(parent: Composite) {
    enableSemanticHighlightingCheckBox = new Button(parent, SWT.CHECK)
    enableSemanticHighlightingCheckBox.setText("Enable semantic highlighting")
    enableSemanticHighlightingCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    enableSemanticHighlightingCheckBox.setSelection(overlayStore.getBoolean(ENABLE_SEMANTIC_HIGHLIGHTING))

    extraAccuracyCheckBox = new Button(parent, SWT.CHECK)
    extraAccuracyCheckBox.setText("Use slower but more accurate semantic highlighting")
    extraAccuracyCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    extraAccuracyCheckBox.setSelection(overlayStore.getBoolean(USE_SYNTACTIC_HINTS))
    extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)

    strikethroughDeprecatedCheckBox = new Button(parent, SWT.CHECK)
    strikethroughDeprecatedCheckBox.setText("Strikethrough deprecated symbols")
    strikethroughDeprecatedCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    strikethroughDeprecatedCheckBox.setSelection(overlayStore.getBoolean(STRIKETHROUGH_DEPRECATED))
    strikethroughDeprecatedCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)

    setUpSelectionListener
  }

  private def setUpSelectionListener {
    enableSemanticHighlightingCheckBox.addSelectionListener { () =>
      overlayStore.setValue(ENABLE_SEMANTIC_HIGHLIGHTING, enableSemanticHighlightingCheckBox.getSelection)
      extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
      strikethroughDeprecatedCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
      handleSyntaxColorListSelection()
    }
    extraAccuracyCheckBox.addSelectionListener { () =>
      overlayStore.setValue(USE_SYNTACTIC_HINTS, extraAccuracyCheckBox.getSelection)
    }
    strikethroughDeprecatedCheckBox.addSelectionListener { () =>
      overlayStore.setValue(STRIKETHROUGH_DEPRECATED, strikethroughDeprecatedCheckBox.getSelection)
    }

  }
}

object SyntaxColoringPreferencePage {

  val PreviewText =
    ("""package foo.bar.baz
        |/**
        | * Scaladoc
        | * @scaladocAnnotation value
        | * $SCALADOC_MACRO
        | * {{{
        | * @annotation.tailrec
        | * def f(i: Int): Int =
        | *   if (i > 0) f(i - 1) else 0
        | * }}}
        | */
        |@Annotation
        |class Class[T] extends Trait {
        |  object Object
        |  case object CaseObject
        |  case class CaseClass
        |  type Type = Int
        |  lazy val lazyTemplateVal = 42
        |  val templateVal = 42
        |  var templateVar = 24
        |  def method(param: Int): Int = {
        |    // Single-line comment
        |    /* Multi-line comment */
        |    lazy val lazyLocalVal = 42
        |    val localVal = "foo\nbar" + """ + "\"\"\"" + "multiline string" + "\"\"\"" + """
        |    var localVar =
        |      <tag attributeName="value">
        |        <!-- XML comment -->
        |        <?processinginstruction?>
        |        <![CDATA[ CDATA ]]>
        |        PCDATA
        |      </tag>
        |    val sym = 'symbol
        |    return 42
        |  }
        |  @deprecated def deprecatedMethod(param: Int) = ???
        |  templateVar = deprecatedMethod(12)
        |  val str = s"Here is a $templateV\u0430l, " +
        |    s"$templateV\u0430r, $p\u0430ram, $$notAVariable"
        |}
        |""").stripMargin

}
