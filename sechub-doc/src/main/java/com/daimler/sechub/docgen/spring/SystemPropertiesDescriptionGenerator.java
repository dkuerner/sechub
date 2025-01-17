// SPDX-License-Identifier: MIT
package com.daimler.sechub.docgen.spring;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.util.StringUtils;

import com.daimler.sechub.docgen.DocAnnotationData;
import com.daimler.sechub.docgen.Generator;
import com.daimler.sechub.docgen.spring.SpringValueExtractor.SpringValue;
import com.daimler.sechub.docgen.spring.SpringValueFilter.AcceptAllSpringValueFilter;

public class SystemPropertiesDescriptionGenerator implements Generator {

	SpringValueExtractor springValueExtractor;

	public SystemPropertiesDescriptionGenerator() {
		this.springValueExtractor = new SpringValueExtractor();
	}

	public String generate(List<DocAnnotationData> list) {
		return generate(list, AcceptAllSpringValueFilter.INSTANCE);
	}

	public String generate(List<DocAnnotationData> list, SpringValueFilter filter) {
		if (list == null || list.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Map<String, SortedSet<TableRow>> rowMap = new TreeMap<>();
		for (DocAnnotationData data : list) {
			if (StringUtils.isEmpty(data.springValue)) {
				continue;
			}
			SpringValue extracted = springValueExtractor.extract(data.springValue);
			if (filter.isFiltered(extracted)) {
				continue;
			}
			TableRow row = new TableRow();
			row.defaultValue = extracted.getDefaultValue();
			row.hasDefaultValue = extracted.hasDefaultValue();
			row.propertyKey = extracted.getKey();
			row.description = data.description;

			SortedSet<TableRow> rows = rowMap.get(data.scope);
			if (rows == null) {
				rows = new TreeSet<>();
				rowMap.put(data.scope, rows);
			}
			rows.add(row);
		}
		if (rowMap.isEmpty()) {
			return "";
		}
		appendStringContent(sb, rowMap);
		return sb.toString();
	}

	protected void appendStringContent(StringBuilder sb, Map<String, SortedSet<TableRow>> rowMap) {
		for (Map.Entry<String, SortedSet<TableRow>> entries : rowMap.entrySet()) {
			SortedSet<TableRow> table = entries.getValue();

			sb.append("[options=\"header\",cols=\"1,1,1\"]\n");
			sb.append(".").append(buildTitle(entries.getKey()));
			sb.append("\n|===\n");
			sb.append("|Key   |Default   |Description \n");
			sb.append("//----------------------\n");
			for (TableRow row : table) {
				sb.append("|").append(row.propertyKey);
				sb.append("|").append(row.defaultValue);
				sb.append("|").append(row.description);
				sb.append("\n");
			}
			sb.append("\n|===\n\n");
		}
	}

	private String buildTitle(String key) {
		return "Scope '" + key + "'";
	}

	class TableRow implements Comparable<TableRow> {
		String propertyKey;
		String defaultValue;
		String description;
		boolean hasDefaultValue;
		
		@Override
		public int compareTo(TableRow o) {
			return getPropertyKey().compareTo(o.getPropertyKey());
		}

		public String getPropertyKey() {
			if (propertyKey == null) {
				return "";
			}
			return propertyKey;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + ((propertyKey == null) ? 0 : propertyKey.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TableRow other = (TableRow) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (defaultValue == null) {
				if (other.defaultValue != null)
					return false;
			} else if (!defaultValue.equals(other.defaultValue))
				return false;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (propertyKey == null) {
				if (other.propertyKey != null)
					return false;
			} else if (!propertyKey.equals(other.propertyKey))
				return false;
			return true;
		}

		private SystemPropertiesDescriptionGenerator getOuterType() {
			return SystemPropertiesDescriptionGenerator.this;
		}
	}
}
