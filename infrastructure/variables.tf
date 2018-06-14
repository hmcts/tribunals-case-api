variable "product" {
  type    = "string"
}

variable "component" {
  type    = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "infrastructure_env" {
  default     = "dev"
  description = "Infrastructure environment to point to"
}

variable "subscription" {
  type = "string"
}

variable "ilbIp"{}

variable "appeal_email_subject" {
  type    = "string"
  default = "Your appeal"
}

variable "appeal_email_message" {
  type    = "string"
  default = "Your appeal has been created. Please do not respond to this email"
}

variable "appeal_email_smtp_tls_enabled" {
  type    = "string"
  default = "true"
}

variable "appeal_email_smtp_ssl_trust" {
  type    = "string"
  default = "*"
}

variable "core_case_data_jurisdiction_id"{
  default = "SSCS"
}

variable "core_case_data_case_type_id"{
  default = "Benefit"
}

variable "ccd_idam_s2s_auth_microservice"{
  default = "sscs"
}

variable "idam_oauth2_client_id"{
  default = "sscs"
}

variable "idam_redirect_url" {
  default = "https://sscs-case-loader-sandbox.service.core-compute-sandbox.internal"
}

variable "robotics_email_subject" {
  type    = "string"
  default = "Robotics Data"
}

variable "robotics_email_message" {
  type    = "string"
  default = "Please find attached the robotics json file \nPlease do not respond to this email"
}

variable "robotics_enabled" {
  type    = "string"
  default = "false"
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type        = "string"
}