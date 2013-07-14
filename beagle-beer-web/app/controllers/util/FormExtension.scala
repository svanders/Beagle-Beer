package controllers.util

import play.api.data.{Mapping, Forms}
import play.api.data.format._
import play.api.data.validation._



/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 28/06/13
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
object FormExtension  {

//  def nonBlank: Mapping[String] = text verifying Contraints.nonEmpty

  /**
   * Defines a ‘required’ constraint for `String` values, i.e. one in which empty strings are invalid.
   *
   * '''name'''[constraint.required]
   * '''error'''[error.required]
   */
  def nonBlank: Constraint[String] = Constraint[String]("constraint.required") { o =>
  // this should be fixed in the next play release, this fix looks to have been merged into master
  // https://github.com/playframework/Play20/commit/41c20e28b63349ad98c11872a32ddd9925bfb125
  // so try removing this method after the next release
    if (o.trim.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }

}
