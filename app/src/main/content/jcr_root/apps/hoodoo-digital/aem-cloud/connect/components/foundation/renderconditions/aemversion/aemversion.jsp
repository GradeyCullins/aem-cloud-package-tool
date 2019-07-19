<%--
  ==============================================================================
  aem version render condition
   A condition that makes the rendering decision based on the system's aem version.
  /**
   * The aem version that should evaluate to true
   */
  - aemVersion (String)
  ==============================================================================
--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.rendercondition.RenderCondition,
                  com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
                  com.adobe.granite.license.ProductInfoService"%>
<%
    ProductInfoService productInfoService = sling.getService(ProductInfoService.class);

    boolean vote = false;

    Config cfg = new Config(resource);
    if (cfg != null && productInfoService != null) {
        String aemVersion = cfg.get("aemVersion", String.class);
        String productAemVersion = productInfoService.getInfos()[0].getVersion().toString();
        vote = productAemVersion.contains(aemVersion);
    }

    /* Display or hide the widget */
    request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));
%>
