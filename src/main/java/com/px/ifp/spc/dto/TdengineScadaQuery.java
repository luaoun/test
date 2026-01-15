package com.px.ifp.spc.dto;

import java.util.List;
import java.util.Map;

public class TdengineScadaQuery {
        private String startTime;
        private String endTime;
        private List<String> fields;
        private String aggregateFunction;
        private String interval;
        private boolean partitionByTbname;
        private List<String> tbNames;
        private Map<String, Object> featureEquals;
        private String deviceId;
        private String location;

        public String getStartTime() {
        return startTime;
    }

        public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

        public String getEndTime() {
        return endTime;
    }

        public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

        public List<String> getFields() {
        return fields;
    }

        public void setFields(List<String> fields) {
        this.fields = fields;
    }

        public String getAggregateFunction() {
        return aggregateFunction;
    }

        public void setAggregateFunction(String aggregateFunction) {
        this.aggregateFunction = aggregateFunction;
    }

        public String getInterval() {
        return interval;
    }

        public void setInterval(String interval) {
        this.interval = interval;
    }

        public boolean isPartitionByTbname() {
        return partitionByTbname;
    }

        public void setPartitionByTbname(boolean partitionByTbname) {
        this.partitionByTbname = partitionByTbname;
    }

        public List<String> getTbNames() {
        return tbNames;
    }

        public void setTbNames(List<String> tbNames) {
        this.tbNames = tbNames;
    }

        public Map<String, Object> getFeatureEquals() {
        return featureEquals;
    }

        public void setFeatureEquals(Map<String, Object> featureEquals) {
        this.featureEquals = featureEquals;
    }

        public String getDeviceId() {
        return deviceId;
    }

        public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

        public String getLocation() {
        return location;
    }

        public void setLocation(String location) {
        this.location = location;
    }
}
