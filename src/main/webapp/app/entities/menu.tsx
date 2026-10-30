import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  return (
    <>
      <MenuItem icon="asterisk" to="/fde">
        FDE 업무 워크벤치
      </MenuItem>
      <MenuItem icon="asterisk" to="/kit">
        장비 점검 예제
      </MenuItem>
      <MenuItem icon="asterisk" to="/organization">
        조직 권한·승인 설정
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
