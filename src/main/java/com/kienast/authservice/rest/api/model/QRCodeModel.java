package com.kienast.authservice.rest.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * QRCodeModel
 */

public class QRCodeModel   {
  @JsonProperty("qrcode")
  private String qrcode;

  public QRCodeModel qrcode(String qrcode) {
    this.qrcode = qrcode;
    return this;
  }

  /**
   * Get qrcode
   * @return qrcode
  */
  @ApiModelProperty(value = "")


  public String getQrcode() {
    return qrcode;
  }

  public void setQrcode(String qrcode) {
    this.qrcode = qrcode;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QRCodeModel qrCode = (QRCodeModel) o;
    return Objects.equals(this.qrcode, qrCode.qrcode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qrcode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QRCodeModel {\n");
    
    sb.append("    qrcode: ").append(toIndentedString(qrcode)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

