{{/*
Expand the name of the chart.
*/}}
{{- define "zendapag.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "zendapag.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "zendapag.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "zendapag.labels" -}}
helm.sh/chart: {{ include "zendapag.chart" . }}
{{ include "zendapag.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
environment: {{ .Values.environment }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "zendapag.selectorLabels" -}}
app.kubernetes.io/name: {{ include "zendapag.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "zendapag.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "zendapag.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
API selector labels
*/}}
{{- define "zendapag.api.selectorLabels" -}}
{{ include "zendapag.selectorLabels" . }}
app.kubernetes.io/component: api
{{- end }}

{{/*
Worker selector labels
*/}}
{{- define "zendapag.worker.selectorLabels" -}}
{{ include "zendapag.selectorLabels" . }}
app.kubernetes.io/component: worker
{{- end }}

{{/*
API labels
*/}}
{{- define "zendapag.api.labels" -}}
{{ include "zendapag.labels" . }}
app.kubernetes.io/component: api
{{- end }}

{{/*
Worker labels
*/}}
{{- define "zendapag.worker.labels" -}}
{{ include "zendapag.labels" . }}
app.kubernetes.io/component: worker
{{- end }}

{{/*
Image name for API
*/}}
{{- define "zendapag.api.image" -}}
{{- $registry := .Values.image.api.registry | default .Values.global.imageRegistry -}}
{{- if $registry -}}
{{ $registry }}/{{ .Values.image.api.repository }}:{{ .Values.image.api.tag }}
{{- else -}}
{{ .Values.image.api.repository }}:{{ .Values.image.api.tag }}
{{- end -}}
{{- end }}

{{/*
Image name for Worker
*/}}
{{- define "zendapag.worker.image" -}}
{{- $registry := .Values.image.worker.registry | default .Values.global.imageRegistry -}}
{{- if $registry -}}
{{ $registry }}/{{ .Values.image.worker.repository }}:{{ .Values.image.worker.tag }}
{{- else -}}
{{ .Values.image.worker.repository }}:{{ .Values.image.worker.tag }}
{{- end -}}
{{- end }}

{{/*
Return the proper Storage Class
*/}}
{{- define "zendapag.storageClass" -}}
{{- if .Values.global.storageClass -}}
{{- .Values.global.storageClass -}}
{{- else -}}
{{- .Values.persistence.storageClass -}}
{{- end -}}
{{- end }}

{{/*
Create environment variables
*/}}
{{- define "zendapag.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: {{ .Values.config.springProfiles | quote }}
- name: JAVA_OPTS
  value: {{ .Values.config.javaOpts | quote }}
- name: LOGGING_LEVEL_COM_ZENDAPAG
  value: {{ .Values.config.logLevel | quote }}
{{- if .Values.externalServices.database.host }}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ .Values.externalServices.database.host }}:{{ .Values.externalServices.database.port }}/{{ .Values.externalServices.database.database }}"
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.externalServices.database.existingSecret }}
      key: {{ .Values.externalServices.database.secretKeys.usernameKey }}
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.externalServices.database.existingSecret }}
      key: {{ .Values.externalServices.database.secretKeys.passwordKey }}
{{- end }}
{{- if .Values.externalServices.redis.host }}
- name: SPRING_DATA_REDIS_HOST
  value: {{ .Values.externalServices.redis.host | quote }}
- name: SPRING_DATA_REDIS_PORT
  value: {{ .Values.externalServices.redis.port | quote }}
{{- if .Values.externalServices.redis.auth.enabled }}
- name: SPRING_DATA_REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.externalServices.redis.existingSecret }}
      key: password
{{- end }}
{{- end }}
{{- if .Values.externalServices.kafka.bootstrapServers }}
- name: SPRING_KAFKA_BOOTSTRAP_SERVERS
  value: {{ .Values.externalServices.kafka.bootstrapServers | quote }}
{{- end }}
- name: APP_JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ .Values.secrets.jwt.secretName }}
      key: {{ .Values.secrets.jwt.secretKey }}
- name: APP_KAFKA_TOPICS_TRANSACTION_EVENTS
  value: {{ .Values.config.kafkaTopics.transactionEvents | quote }}
- name: APP_KAFKA_TOPICS_PIX_WEBHOOK
  value: {{ .Values.config.kafkaTopics.pixWebhook | quote }}
- name: APP_KAFKA_TOPICS_NOTIFICATIONS
  value: {{ .Values.config.kafkaTopics.notifications | quote }}
{{- end }}

{{/*
Volume mounts
*/}}
{{- define "zendapag.volumeMounts" -}}
- name: pix-certs
  mountPath: /app/certs
  readOnly: true
- name: tmp
  mountPath: /tmp
- name: logs
  mountPath: /app/logs
{{- if .Values.persistence.enabled }}
- name: data
  mountPath: /app/data
{{- end }}
{{- end }}

{{/*
Volumes
*/}}
{{- define "zendapag.volumes" -}}
- name: pix-certs
  secret:
    secretName: {{ .Values.secrets.pixCerts.secretName }}
    defaultMode: 0400
- name: tmp
  emptyDir: {}
- name: logs
  emptyDir: {}
{{- if .Values.persistence.enabled }}
- name: data
  persistentVolumeClaim:
    claimName: {{ include "zendapag.fullname" . }}-data
{{- end }}
{{- end }}

{{/*
Security context
*/}}
{{- define "zendapag.securityContext" -}}
{{- toYaml .Values.securityContext }}
{{- end }}

{{/*
Container security context
*/}}
{{- define "zendapag.containerSecurityContext" -}}
{{- toYaml .Values.containerSecurityContext }}
{{- end }}

{{/*
Pod anti-affinity
*/}}
{{- define "zendapag.podAntiAffinity" -}}
{{- if .Values.affinity.podAntiAffinity.enabled -}}
{{- if eq .Values.affinity.podAntiAffinity.type "hard" -}}
requiredDuringSchedulingIgnoredDuringExecution:
{{- else -}}
preferredDuringSchedulingIgnoredDuringExecution:
{{- end }}
- weight: 100
  podAffinityTerm:
    labelSelector:
      matchExpressions:
      - key: app.kubernetes.io/component
        operator: In
        values: [{{ .component | quote }}]
    topologyKey: kubernetes.io/hostname
{{- end -}}
{{- end }}

{{/*
Topology spread constraints
*/}}
{{- define "zendapag.topologySpreadConstraints" -}}
{{- if .Values.topologySpreadConstraints.enabled -}}
- maxSkew: {{ .Values.topologySpreadConstraints.maxSkew }}
  topologyKey: {{ .Values.topologySpreadConstraints.topologyKey }}
  whenUnsatisfiable: {{ .Values.topologySpreadConstraints.whenUnsatisfiable | default "DoNotSchedule" }}
  labelSelector:
    matchLabels:
      {{- include "zendapag.selectorLabels" . | nindent 6 }}
      app.kubernetes.io/component: {{ .component }}
{{- end -}}
{{- end }}