<#macro task path create task>

<script type="text/javascript">
    var map, home;

    DG.then(function () {
        <#if task != 'null' && task.lat?? && task.lng??>
            map = DG.map('map', {
                center: [${task.lat}, ${task.lng}],
                zoom: 17
            });
            home = DG.marker([${task.lat}, ${task.lng}], {
                draggable: true
            }).addTo(map);
        <#else>
            map = DG.map('map', {
                center: [50.45, 30.52],//Kyiv coordinates
                zoom: 12
            });
        </#if>

        map.on('click', function(e) {
            if (!home) {
                home = DG.marker([50.45, 30.52], {
                    draggable: true
                }).addTo(map);
            }
            home.setLatLng([e.latlng.lat, e.latlng.lng]);
        });
    });

    function validateMap() {
        if (!home) {
            alert("Вы забыли указать местоположение задания на карте!");
        } else {
            $("#lat").val(home.getLatLng().lat);
            $("#lng").val(home.getLatLng().lng);
            $("#newtask").submit();
        }
    }

    $(function () {
        $("#execFrom").datetimepicker({
            sideBySide: true,
            locale: 'ru'<#if task != 'null' && task.execFrom??>,
            date: new Date('${task.execFrom}')</#if>
        });
        $("#execTo").datetimepicker({
            sideBySide: true,
            useCurrent: false,
            locale: 'ru'<#if task != 'null' && task.execTo??>,
            date: new Date('${task.execTo}')</#if>
        });
        $("#execFrom").on("change.datetimepicker", function (e) {
            $('#execTo').datetimepicker('minDate', e.date);
        });
        $("#execTo").on("change.datetimepicker", function (e) {
            $('#execFrom').datetimepicker('maxDate', e.date);
        });
    });
</script>

<form action="${path}" method="post" onsubmit="if (!validateMap(this)) event.preventDefault();" id="newtask">
    <div class="form-group">
        <label>Категория:</label>
        <select class="form-control" name="category" required>
            <#list categories as parent, children>
            <optgroup label="${parent.name}">
                <#list children as child>
                    <option value="${child.id}" <#if task != 'null' && task.category.id == child.id>selected</#if>>${child.name}</option>
                </#list>
            </optgroup>
            </#list>
        </select>
    </div>

    <div class="form-group">
        <label>Заголовок:</label>
        <input type="text" class="form-control${(titleError??)?string(' is-invalid', '')}"
               value="<#if task != 'null'>${task.title}</#if>" name="title" size="40"
               placeholder="Что нужно сделать?" required/>
        <#if titleError??>
            <div class="invalid-feedback">
                ${titleError}
            </div>
        </#if>
    </div>

    <div class="form-group">
        <label>Подробнее:</label>
        <textarea class="form-control${(descriptionError??)?string(' is-invalid', '')}"
                  rows="10" name="description" placeholder="Подробно опишите ваше задание" required><#if task != 'null'>${task.description}</#if></textarea>
        <#if descriptionError??>
            <div class="invalid-feedback">
                ${descriptionError}
            </div>
        </#if>
    </div>

    <div class="row">
        <div class="col">
            <div class="mb-2">Местоположение задания:</div>
            <div id="map" class="w-100 h-75"></div>
            <div class="form-group my-3">
                <button type="submit" class="btn btn-primary"><#if create>Опубликовать<#else>Сохранить</#if></button>
            </div>
        </div>

        <div class="col">
            <div class="form-group">
                <label>Конфиденциальные данные: (не обязательно):</label>
                <textarea class="form-control${(secretError??)?string(' is-invalid', '')}" rows="5" name="secret" placeholder="Эта информация будет доступна только выбранному исполнителю. Укажите здесь Ваш номер телефона, номер подъезда и квартиры, дополнительные контакты и пр."><#if task != 'null'>${task.secret}</#if></textarea>
            <#if secretError??>
                <div class="invalid-feedback">
                    ${secretError}
                </div>
            </#if>
            </div>

            <div class="form-group mb-1">
                <label>Дата выполнения задания:</label>
                <input type="text" name="execFrom"
                       class="form-control datetimepicker-input${(execFromError?? || fromBeforeToError??)?string(' is-invalid', '')}"
                       id="execFrom" data-toggle="datetimepicker"
                       data-target="#execFrom" autocomplete="off"
                       placeholder="От" required/>
            <#if execFromError??>
                <div class="invalid-feedback">
                    ${execFromError}
                </div>
                <#elseif fromBeforeToError??>
                <div class="invalid-feedback">
                    ${fromBeforeToError}
                </div>
            </#if>
            </div>
            <div class="form-group">
                <input type="text" name="execTo"
                       class="form-control datetimepicker-input${(execToError?? || fromBeforeToError??)?string(' is-invalid', '')}"
                       id="execTo" data-toggle="datetimepicker"
                       data-target="#execTo" autocomplete="off"
                       placeholder="До" required/>
            </div>

            <div class="form-inline mb-2">
                <label>Оплата работы:</label>
                <input type="number" name="price" value="<#if task != 'null'>${task.price}</#if>" class="form-control mx-2" min="0" placeholder="Цена" required/>грн
            </div>

            <div class="form-group">
                <label>Наличный или безналичный расчёт:</label>
                <div class="btn-group btn-group-toggle" data-toggle="buttons">
                    <label class="btn btn-outline-dark<#if task != 'null' && !task.cashless || task == 'null'> active</#if>">
                        <input type="radio" name="cashless" id="cashlessFalse" value="false" autocomplete="off"<#if task != 'null' && !task.cashless || task == 'null'> checked</#if>> Наличный
                    </label>
                    <label class="btn btn-outline-dark<#if task != 'null' && task.cashless> active</#if>">
                        <input type="radio" name="cashless" id="cashlessTrue" value="true" autocomplete="off"<#if !hasCreditCard> disabled<#elseif task != 'null' && task.cashless> checked</#if>> Безналичный
                    </label>
                </div>
            </div>
        </div>
    </div>

    <input type="hidden" name="lat" id="lat">
    <input type="hidden" name="lng" id="lng">
    <input type="hidden" name="_csrf" value="${_csrf.token}" />
</form>
</#macro>