package views.helper.custom

import views.html.helper.FieldConstructor
import views.html.helper.custom.tableInputNoLabel

/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 25/06/13
 * Time: 6:41 PM
 * To change this template use File | Settings | File Templates.
 */
object CustomHelpers {


  implicit val wrapedInTdNoLabel = FieldConstructor(tableInputNoLabel.f)
}
